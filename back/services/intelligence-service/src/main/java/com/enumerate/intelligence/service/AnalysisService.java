package com.enumerate.intelligence.service;

import com.enumerate.common.core.exception.BizException;
import com.enumerate.common.core.result.ResultCode;
import com.enumerate.intelligence.dto.ArticleStatsVO;
import com.enumerate.intelligence.dto.RecommendationVO;
import com.enumerate.intelligence.entity.ArticleAnalysis;
import com.enumerate.intelligence.mapper.AnalysisMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisMapper analysisMapper;
    private final DataSource dataSource;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern IMG_TAG = Pattern.compile("<img[^>]+>");
    private static final Pattern PARAGRAPH = Pattern.compile("<p[^>]*>|</p>|<br\\s*/?>");

    public String generateSummary(Long articleId) {
        // Check cached analysis
        ArticleAnalysis existing = analysisMapper.findByArticleId(articleId);
        if (existing != null && existing.getSummary() != null && !existing.getSummary().isBlank()) {
            return existing.getSummary();
        }

        // Get article content from articles table
        String content = getArticleContent(articleId);
        if (content == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "文章不存在");
        }

        String text = stripHtml(content);
        String summary = extractSummary(text);
        return summary;
    }

    public ArticleStatsVO getArticleStats(Long articleId) {
        ArticleAnalysis existing = analysisMapper.findByArticleId(articleId);
        if (existing != null) {
            List<String> keywords = new ArrayList<>();
            try {
                if (existing.getKeywords() != null && !existing.getKeywords().isBlank()) {
                    keywords = MAPPER.readValue(existing.getKeywords(), new TypeReference<List<String>>() {});
                }
            } catch (Exception ignored) {}
            return ArticleStatsVO.builder()
                    .wordCount(existing.getWordCount())
                    .readingTimeMinutes(existing.getReadingTimeMinutes())
                    .keywords(keywords)
                    .build();
        }

        String content = getArticleContent(articleId);
        if (content == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "文章不存在");
        }

        String text = stripHtml(content);
        int wordCount = text.length();
        int readingTimeMinutes = Math.max(1, wordCount / 500);
        List<String> keywords = extractKeywords(text);
        int paragraphCount = content.split("</p>").length;
        int imageCount = countMatches(content, IMG_TAG);

        // Cache analysis result
        ArticleAnalysis analysis = new ArticleAnalysis();
        analysis.setArticleId(articleId);
        analysis.setSummary(extractSummary(text));
        analysis.setWordCount(wordCount);
        analysis.setReadingTimeMinutes(readingTimeMinutes);
        try {
            analysis.setKeywords(MAPPER.writeValueAsString(keywords));
        } catch (Exception ignored) {}

        try {
            if (existing != null) {
                analysisMapper.update(analysis);
            } else {
                analysisMapper.insert(analysis);
            }
        } catch (Exception e) {
            log.warn("缓存分析结果失败: {}", e.getMessage());
        }

        return ArticleStatsVO.builder()
                .wordCount(wordCount)
                .readingTimeMinutes(readingTimeMinutes)
                .paragraphCount(paragraphCount)
                .imageCount(imageCount)
                .keywords(keywords)
                .build();
    }

    public List<RecommendationVO> getRecommendations(Long articleId, int limit) {
        // Get source article tags
        String sourceTagsJson = getArticleTagsJson(articleId);
        List<String> sourceTags = new ArrayList<>();
        if (sourceTagsJson != null && !sourceTagsJson.isBlank()) {
            try { sourceTags = MAPPER.readValue(sourceTagsJson, new TypeReference<List<String>>() {}); }
            catch (Exception ignored) {}
        }

        // Get all other articles tags
        List<Map<String, Object>> articles = analysisMapper.findArticlesForRecommendation(articleId);
        List<RecommendationVO> recommendations = new ArrayList<>();

        for (Map<String, Object> row : articles) {
            Long id = ((Number) row.get("id")).longValue();
            String tagsJson = (String) row.get("tags_json");
            List<String> tags = new ArrayList<>();
            if (tagsJson != null && !tagsJson.isBlank()) {
                try { tags = MAPPER.readValue(tagsJson, new TypeReference<List<String>>() {}); }
                catch (Exception ignored) {}
            }

            // Calculate Jaccard similarity
            double score = jaccardSimilarity(sourceTags, tags);
            if (score > 0) {
                // Get article title
                String title = getArticleTitle(id);
                recommendations.add(RecommendationVO.builder()
                        .id(id).title(title != null ? title : "未知文章")
                        .tags(tags).score(score).build());
            }
        }

        recommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return recommendations.stream().limit(limit).collect(Collectors.toList());
    }

    public List<String> suggestTags(Long articleId, String content) {
        String text;
        if (articleId != null) {
            text = getArticleContent(articleId);
            if (text == null) {
                throw new BizException(ResultCode.NOT_FOUND.getCode(), "文章不存在");
            }
            text = stripHtml(text);
        } else if (content != null && !content.isBlank()) {
            text = stripHtml(content);
        } else {
            return List.of();
        }

        // Extract keywords and match against existing tags
        List<String> extracted = extractKeywords(text);

        // Get existing tags from all articles
        Set<String> existingTags = new HashSet<>();
        List<Map<String, Object>> all = analysisMapper.findAllArticleIdsAndTags();
        for (Map<String, Object> row : all) {
            String tagsJson = (String) row.get("tags_json");
            if (tagsJson != null && !tagsJson.isBlank()) {
                try {
                    existingTags.addAll(MAPPER.readValue(tagsJson, new TypeReference<List<String>>() {}));
                } catch (Exception ignored) {}
            }
        }

        existingTags = existingTags.stream().map(String::toLowerCase).collect(Collectors.toSet());

        // Match extracted keywords with existing tags
        List<String> suggestions = new ArrayList<>();
        for (String keyword : extracted) {
            if (existingTags.contains(keyword.toLowerCase())) {
                suggestions.add(keyword);
            }
        }

        // Also suggest top extracted keywords
        for (String keyword : extracted) {
            if (!suggestions.contains(keyword)) {
                suggestions.add(keyword);
            }
            if (suggestions.size() >= 5) break;
        }

        return suggestions;
    }

    private String getArticleContent(Long articleId) {
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            List<String> results = jdbc.queryForList(
                    "SELECT content FROM articles WHERE id = ?", String.class, articleId);
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            log.error("获取文章内容失败: {}", e.getMessage());
            return null;
        }
    }

    private String getArticleTitle(Long articleId) {
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            List<String> results = jdbc.queryForList(
                    "SELECT title FROM articles WHERE id = ?", String.class, articleId);
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private String getArticleTagsJson(Long articleId) {
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            List<String> results = jdbc.queryForList(
                    "SELECT tags_json FROM articles WHERE id = ?", String.class, articleId);
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        String text = HTML_TAG.matcher(html).replaceAll(" ");
        return text.replaceAll("\\s+", " ").trim();
    }

    private String extractSummary(String text) {
        // Take first 200 characters as summary
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

    private List<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) return List.of();

        // Stop words
        Set<String> stopWords = Set.of("的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都",
                "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看",
                "好", "自己", "这", "他", "她", "它", "们", "那", "里", "为", "与", "及", "但", "而",
                "或", "被", "把", "对", "从", "以", "将", "能", "可以", "这个", "那个", "一个", "我们");

        // Simple Chinese text word frequency counting
        Map<String, Integer> freq = new HashMap<>();
        // Extract 2-4 character word candidates
        for (int i = 0; i < text.length() - 1; i++) {
            for (int j = i + 2; j <= Math.min(i + 4, text.length()); j++) {
                String word = text.substring(i, j);
                if (word.length() >= 2 && !stopWords.contains(word) && isChinese(word)) {
                    freq.merge(word, 1, Integer::sum);
                }
            }
        }

        return freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private boolean isChinese(String text) {
        return text.chars().allMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN || c == 0x3000 || c == 0x3001 || c == 0x3002);
    }

    private double jaccardSimilarity(List<String> a, List<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0;
        Set<String> setA = new HashSet<>(a.stream().map(String::toLowerCase).collect(Collectors.toList()));
        Set<String> setB = new HashSet<>(b.stream().map(String::toLowerCase).collect(Collectors.toList()));
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);
        return (double) intersection.size() / union.size();
    }

    private int countMatches(String text, Pattern pattern) {
        if (text == null) return 0;
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }
}
