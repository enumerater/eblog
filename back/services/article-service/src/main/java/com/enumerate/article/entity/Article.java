package com.enumerate.article.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class Article {

    private Long id;
    private String title;
    private String content;
    private String tagsJson;
    private String summary;
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

    public void setTags(List<String> tags) {
        try {
            this.tagsJson = (tags == null || tags.isEmpty()) ? "[]" : MAPPER.writeValueAsString(tags);
        } catch (Exception e) {
            this.tagsJson = "[]";
        }
    }

    public static String generateSummary(String html) {
        if (html == null) return "";
        String text = html.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
        if (text.length() <= 150) return text;
        return text.substring(0, 150);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getTagsJson() { return tagsJson; }
    public void setTagsJson(String tagsJson) { this.tagsJson = tagsJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
