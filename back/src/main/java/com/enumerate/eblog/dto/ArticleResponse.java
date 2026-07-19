package com.enumerate.eblog.dto;

import com.enumerate.eblog.entity.Article;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ArticleResponse {

    private Long id;
    private String title;
    private String content;
    private List<String> tags;
    private String summary;
    private String createdAt;
    private String updatedAt;

    public static ArticleResponse from(Article article) {
        ArticleResponse r = new ArticleResponse();
        r.id = article.getId();
        r.title = article.getTitle();
        r.content = article.getContent();
        r.tags = article.getTags();
        r.summary = article.getSummary();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        r.createdAt = article.getCreatedAt() != null ? article.getCreatedAt().format(fmt) : null;
        r.updatedAt = article.getUpdatedAt() != null ? article.getUpdatedAt().format(fmt) : null;
        return r;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public List<String> getTags() { return tags; }
    public String getSummary() { return summary; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}