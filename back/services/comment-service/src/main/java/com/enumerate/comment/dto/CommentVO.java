package com.enumerate.comment.dto;

import com.enumerate.comment.entity.Comment;
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
public class CommentVO {
    private Long id;
    private Long articleId;
    private Long parentId;
    private String author;
    private String content;
    private String status;
    private String avatarUrl;
    private Long userId;
    /** 回复目标 (当回复的不是顶级评论时，显示"回复 @XXX") */
    private String replyToName;
    private LocalDateTime createdAt;
    private List<CommentVO> replies;

    public static CommentVO from(Comment comment) {
        return CommentVO.builder()
                .id(comment.getId())
                .articleId(comment.getArticleId())
                .parentId(comment.getParentId())
                .author(comment.getAuthor())
                .content(comment.getContent())
                .status(comment.getStatus())
                .avatarUrl(comment.getAvatarUrl())
                .userId(comment.getUserId())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}