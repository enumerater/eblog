package com.enumerate.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 评论事件消息体 — 通过 RocketMQ 从 comment-service 推送至 notification-service
 *
 * 触发场景:
 *  用户在文章下发表评论或回复 → comment-service publish → MQ → notification-service consume
 *  通知服务根据此消息创建站内通知（通知文章作者有人评论了）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentEventDTO implements Serializable {

    /** 事件类型: COMMENT_CREATED */
    private String eventType;

    /** 评论 ID */
    private Long commentId;

    /** 评论所属文章 ID */
    private Long articleId;

    /** 文章的标题（便于通知展示） */
    private String articleTitle;

    /** 评论者用户 ID（0=匿名） */
    private Long commentUserId;

    /** 评论者昵称 */
    private String author;

    /** 评论内容 */
    private String content;

    /** 父评论 ID（null=顶级评论） */
    private Long parentId;

    /** 事件发生时间戳 */
    private long timestamp;
}