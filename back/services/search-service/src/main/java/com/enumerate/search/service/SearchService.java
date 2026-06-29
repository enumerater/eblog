package com.enumerate.search.service;

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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchMapper searchMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final DataSource dataSource;

    private static final String HOT_SEARCH_KEY = "search:hot";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public List<SearchResultVO> searchArticles(String keyword, String tag, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 10;

        int offset = (page - 1) * size;
        List<SearchResultVO> results = new ArrayList<>();

        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            StringBuilder sql = new StringBuilder(
                    "SELECT id, title, content, tags_json, created_at FROM articles WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                sql.append(" AND (title LIKE ? OR content LIKE ?)");
                params.add("%" + keyword + "%");
                params.add("%" + keyword + "%");
            }
            if (tag != null && !tag.isBlank()) {
                sql.append(" AND tags_json LIKE ?");
                params.add("%" + tag + "%");
            }
            sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
            params.add(size);
            params.add(offset);

            List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());

            for (Map<String, Object> row : rows) {
                Long id = ((Number) row.get("id")).longValue();
                String title = (String) row.get("title");
                String content = (String) row.get("content");
                String tagsJson = (String) row.get("tags_json");
                Date createdAt = (Date) row.get("created_at");

                List<String> tags = new ArrayList<>();
                if (tagsJson != null && !tagsJson.isBlank()) {
                    try { tags = MAPPER.readValue(tagsJson, new TypeReference<List<String>>() {}); } catch (Exception ignored) {}
                }

                String plainText = content != null ? content.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim() : "";
                String summary = plainText.length() > 150 ? plainText.substring(0, 150) + "..." : plainText;
                String highlight = generateHighlight(plainText, keyword);

                results.add(SearchResultVO.builder()
                        .id(id).title(title).summary(summary)
                        .highlight(highlight).tags(tags)
                        .createdAt(new java.sql.Timestamp(createdAt.getTime()).toLocalDateTime())
                        .build());
            }
        } catch (Exception e) {
            log.error("搜索查询失败: {}", e.getMessage());
        }

        // 记录搜索日志
        if (keyword != null && !keyword.isBlank()) {
            try {
                SearchLog log = new SearchLog();
                log.setKeyword(keyword);
                log.setResultCount(results.size());
                searchMapper.insertLog(log);
                stringRedisTemplate.opsForZSet().incrementScore(HOT_SEARCH_KEY, keyword, 1);
            } catch (Exception e) {
                log.warn("记录搜索日志失败: {}", e.getMessage());
            }
        }

        return results;
    }

    public List<SearchSuggestionVO> getSuggestions(String prefix) {
        if (prefix == null || prefix.isBlank()) return List.of();
        return searchMapper.findTitleSuggestions(prefix).stream()
                .map(t -> SearchSuggestionVO.builder().keyword(t).type("article").build())
                .limit(10)
                .collect(Collectors.toList());
    }

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

    private String generateHighlight(String text, String keyword) {
        if (text == null || keyword == null || keyword.isBlank()) return "";
        int idx = text.toLowerCase().indexOf(keyword.toLowerCase());
        if (idx < 0) return text.substring(0, Math.min(200, text.length()));
        int start = Math.max(0, idx - 50);
        int end = Math.min(text.length(), idx + keyword.length() + 80);
        String snippet = (start > 0 ? "..." : "") + text.substring(start, end) + (end < text.length() ? "..." : "");
        return snippet.replaceAll("(?i)(" + keyword.replaceAll("[.*+?^${}()|[\\]\\\\]", "\\\\$0") + ")", "<mark>$1</mark>");
    }
}