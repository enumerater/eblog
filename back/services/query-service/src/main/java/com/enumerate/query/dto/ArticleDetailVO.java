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
public class ArticleDetailVO {
    private Long id;
    private String title;
    private String content;
    private String summary;
    private List<String> tags;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ArticleDetailVO from(Article article) {
        return from(article, null);
    }

    public static ArticleDetailVO from(Article article, Long viewCount) {
        return ArticleDetailVO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .content(article.getContent())
                .summary(article.getSummary())
                .tags(article.getTags())
                .viewCount(viewCount != null ? viewCount : 0)
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }
}