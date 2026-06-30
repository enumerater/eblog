package com.enumerate.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.enumerate.search.document.ArticleDocument;
import com.enumerate.search.dto.HotSearchVO;
import com.enumerate.search.dto.SearchResultVO;
import com.enumerate.search.dto.SearchSuggestionVO;
import com.enumerate.search.entity.SearchLog;
import com.enumerate.search.mapper.SearchMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索服务
 *
 * 搜索引擎: Elasticsearch 8.x (全文检索 + 高亮)
 * 存储内容: ES 中仅存纯文本 (HTML 已被剥离), 方便搜索和高亮
 * 辅助存储: Redis ZSet (热搜榜), MySQL (搜索日志)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchClient esClient;
    private final SearchMapper searchMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final DataSource dataSource;

    private static final String HOT_SEARCH_KEY = "search:hot";
    private static final String ES_INDEX = "articles";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * ES 全文检索
     *
     * 搜索策略:
     *   1. 有关键词 → multi_match 查询 (title^3, tags^2, content)
     *   2. 有标签 → term 过滤
     *   3. 无关键词 → 按时间排序全量
     *   4. 高亮: title + content 中命中词前后加 <mark> 标签
     */
    public List<SearchResultVO> searchArticles(String keyword, String tag, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 10;
        int from = (page - 1) * size;
        int finalSize = size;

        try {
            var boolBuilder = new co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder();

            if (keyword != null && !keyword.isBlank()) {
                boolBuilder.must(m -> m.multiMatch(mm -> mm
                        .query(keyword)
                        .fields("title^3", "tags^2", "content")
                        .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.MostFields)
                ));
            } else {
                boolBuilder.must(m -> m.matchAll(t -> t));
            }

            if (tag != null && !tag.isBlank()) {
                boolBuilder.filter(f -> f.term(t -> t.field("tags").value(tag)));
            }

            Query boolQuery = boolBuilder.build()._toQuery();

            var highlightBuilder = new co.elastic.clients.elasticsearch.core.search.Highlight.Builder();
            highlightBuilder.fields("title", new HighlightField.Builder().build());
            highlightBuilder.fields("content", new HighlightField.Builder().build());
            highlightBuilder.preTags("<mark>").postTags("</mark>");

            SearchResponse<ArticleDocument> response = esClient.search(s -> s
                            .index(ES_INDEX)
                            .query(boolQuery)
                            .from(from)
                            .size(finalSize)
                            .highlight(highlightBuilder.build()),
                    ArticleDocument.class
            );

            List<SearchResultVO> results = new ArrayList<>();
            for (Hit<ArticleDocument> hit : response.hits().hits()) {
                ArticleDocument doc = hit.source();
                if (doc == null) continue;

                Map<String, List<String>> highlights = hit.highlight();
                // ES 中存的是纯文本, 高亮片段直接可用
                String highlightedTitle = getHighlightOrField(highlights, "title", doc.getTitle());
                String highlightedContent = getHighlightOrField(highlights, "content", doc.getContent() != null ? doc.getContent() : "");
                String summary = highlightedContent.length() > 120
                        ? highlightedContent.substring(0, 120) + "..."
                        : highlightedContent;

                LocalDateTime createdAt = parseDateTime(doc.getCreatedAt());

                results.add(SearchResultVO.builder()
                        .id(doc.getId())
                        .title(highlightedTitle)
                        .summary(summary)
                        .highlight(highlightedTitle)
                        .tags(doc.getTags() != null ? doc.getTags() : List.of())
                        .createdAt(createdAt)
                        .build());
            }

            if (keyword != null && !keyword.isBlank()) {
                recordSearchLog(keyword, results.size());
            }

            return results;

        } catch (Exception e) {
            log.error("ES 搜索失败: keyword={}, error={}", keyword, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 搜索建议 — ES 前缀匹配 title 字段
     */
    public List<SearchSuggestionVO> getSuggestions(String prefix) {
        if (prefix == null || prefix.isBlank()) return List.of();

        try {
            SearchResponse<ArticleDocument> response = esClient.search(s -> s
                            .index(ES_INDEX)
                            .query(q -> q.prefix(p -> p.field("title").value(prefix)))
                            .size(10),
                    ArticleDocument.class
            );

            return response.hits().hits().stream()
                    .map(hit -> {
                        ArticleDocument doc = hit.source();
                        return SearchSuggestionVO.builder()
                                .keyword(doc != null ? doc.getTitle() : "")
                                .type("article")
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("ES 搜索建议失败: prefix={}, fallback to DB, error={}", prefix, e.getMessage());
            return searchMapper.findTitleSuggestions(prefix).stream()
                    .map(t -> SearchSuggestionVO.builder().keyword(t).type("article").build())
                    .limit(10)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 热搜榜 — Redis ZSet
     */
    public List<HotSearchVO> getHotSearches(int limit) {
        Set<String> keywords = stringRedisTemplate.opsForZSet()
                .reverseRangeByScore(HOT_SEARCH_KEY, 0, Double.MAX_VALUE, 0, limit);
        if (keywords == null || keywords.isEmpty()) {
            return searchMapper.findDistinctKeywords(limit).stream()
                    .map(k -> HotSearchVO.builder().keyword(k).count(0).build())
                    .collect(Collectors.toList());
        }
        return keywords.stream()
                .map(k -> {
                    Double score = stringRedisTemplate.opsForZSet().score(HOT_SEARCH_KEY, k);
                    return HotSearchVO.builder().keyword(k).count(score != null ? score.intValue() : 0).build();
                })
                .collect(Collectors.toList());
    }

    // ─────────────────── 全量重索引 ───────────────────

    /**
     * 从 MySQL 全量读取文章并重建 ES 索引
     * 注意: ES 中存储纯文本 (HTML 已剥离), 以便高亮正确工作
     */
    public int reindexAll() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, title, content, tags_json, summary, created_at, updated_at FROM articles ORDER BY id");

        int count = 0;
        for (Map<String, Object> row : rows) {
            try {
                Long id = ((Number) row.get("id")).longValue();
                String title = (String) row.get("title");
                String htmlContent = (String) row.get("content");
                String tagsJson = (String) row.get("tags_json");
                String summary = (String) row.get("summary");
                String createdAt = formatDateTime(row.get("created_at"));
                String updatedAt = formatDateTime(row.get("updated_at"));
                List<String> tags = parseTags(tagsJson);

                // ES 只存纯文本, 高亮才能正确工作
                String plainContent = stripHtml(htmlContent);

                ArticleDocument doc = ArticleDocument.builder()
                        .id(id).title(title).content(plainContent)
                        .summary(summary != null ? summary : generateSummary(htmlContent))
                        .tags(tags).createdAt(createdAt).updatedAt(updatedAt)
                        .build();

                esClient.index(i -> i
                        .index(ES_INDEX)
                        .id(String.valueOf(id))
                        .document(doc));
                count++;
            } catch (Exception e) {
                log.warn("重索引失败: articleId={}, error={}", row.get("id"), e.getMessage());
            }
        }

        log.info("ES 全量重索引完成: 共 {} 篇 (纯文本)", count);
        return count;
    }

    // ─────────────────── 内部方法 ───────────────────

    private LocalDateTime parseDateTime(String isoStr) {
        if (isoStr == null || isoStr.isBlank()) return null;
        try {
            if (isoStr.length() == 10) {
                return LocalDateTime.parse(isoStr + "T00:00:00", DT_FMT);
            }
            return LocalDateTime.parse(isoStr, DT_FMT);
        } catch (Exception e) {
            log.warn("日期解析失败: {}", isoStr);
            return null;
        }
    }

    private String formatDateTime(Object dbValue) {
        if (dbValue == null) return null;
        if (dbValue instanceof LocalDateTime ldt) {
            return ldt.format(DT_FMT);
        }
        if (dbValue instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().format(DT_FMT);
        }
        return dbValue.toString();
    }

    private String getHighlightOrField(Map<String, List<String>> highlights, String field, String fallback) {
        if (highlights != null && highlights.containsKey(field)) {
            List<String> fragments = highlights.get(field);
            if (fragments != null && !fragments.isEmpty()) {
                return String.join("...", fragments);
            }
        }
        return fallback != null ? fallback : "";
    }

    /** HTML → 纯文本 */
    public static String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
    }

    private void recordSearchLog(String keyword, int resultCount) {
        try {
            SearchLog log = new SearchLog();
            log.setKeyword(keyword);
            log.setResultCount(resultCount);
            searchMapper.insertLog(log);
            stringRedisTemplate.opsForZSet().incrementScore(HOT_SEARCH_KEY, keyword, 1);
        } catch (Exception e) {
            log.warn("记录搜索日志失败: {}", e.getMessage());
        }
    }

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) return List.of();
        try {
            return MAPPER.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String generateSummary(String html) {
        if (html == null || html.isBlank()) return "";
        String text = stripHtml(html);
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }
}