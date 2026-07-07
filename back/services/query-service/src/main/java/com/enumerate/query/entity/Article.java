package com.enumerate.query.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Article {
    private Long id;
    private String title;
    private String content;
    private String tagsJson;
    private String summary;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public List<String> getTags() {
        if (tagsJson == null || tagsJson.isBlank()) return new ArrayList<>();
        try {
            return MAPPER.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static String generateSummary(String html) {
        if (html == null || html.isBlank()) return "";
        String text = html.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
        return text.length() > 150 ? text.substring(0, 150) + "..." : text;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTagsJson() { return tagsJson; }
    public void setTagsJson(String tagsJson) { this.tagsJson = tagsJson; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}