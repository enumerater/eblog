package com.enumerate.article.DTO;

import com.enumerate.article.Entity.Draft;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DraftResponse {

    private Long id;
    private String title;
    private String content;
    private List<String> tags;
    private String summary;
    private Long articleId;
    private String createdAt;
    private String updatedAt;

    public static DraftResponse from(Draft draft) {
        DraftResponse r = new DraftResponse();
        r.id = draft.getId();
        r.title = draft.getTitle();
        r.content = draft.getContent();
        r.tags = draft.getTags();
        r.summary = draft.getSummary();
        r.articleId = draft.getArticleId();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        r.createdAt = draft.getCreatedAt() != null ? draft.getCreatedAt().format(fmt) : null;
        r.updatedAt = draft.getUpdatedAt() != null ? draft.getUpdatedAt().format(fmt) : null;
        return r;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public List<String> getTags() { return tags; }
    public String getSummary() { return summary; }
    public Long getArticleId() { return articleId; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
