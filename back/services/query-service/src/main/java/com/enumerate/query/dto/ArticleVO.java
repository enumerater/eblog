package com.enumerate.query.dto;

import com.enumerate.query.entity.Article;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleVO {
    private Long id;
    private String title;
    private String summary;
    private List<String> tags;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ArticleVO from(Article article) {
        return from(article, null);
    }

    public static ArticleVO from(Article article, Long viewCount) {
        return ArticleVO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .summary(article.getSummary() != null && !article.getSummary().isBlank()
                        ? article.getSummary()
                        : Article.generateSummary(article.getContent()))
                .tags(article.getTags())
                .viewCount(viewCount != null ? viewCount : 0)
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }
}