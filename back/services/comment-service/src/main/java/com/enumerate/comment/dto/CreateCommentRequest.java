package com.enumerate.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentRequest {
    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    private Long parentId;

    @NotBlank(message = "评论者昵称不能为空")
    @Size(max = 30, message = "昵称最多30个字符")
    private String author;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容最多1000个字符")
    private String content;
}