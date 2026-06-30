package com.enumerate.notification.mq;

import com.enumerate.common.dto.CommentEventDTO;
import com.enumerate.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消费者 — 评论事件
 *
 * 监听 comment-events topic，消费 comment-service 推送的评论创建事件
 * 收到事件后创建站内通知（通知文章作者有人评论了）
 *
 * 消费失败自动重试（RocketMQ 默认 16 次），超过后进入死信队列
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "comment-events",
        consumerGroup = "comment-notification-consumer"
)
public class CommentEventConsumer implements RocketMQListener<CommentEventDTO> {

    private final NotificationService notificationService;

    @Override
    public void onMessage(CommentEventDTO event) {
        log.info("收到评论事件: commentId={}, articleId={}, author={}",
                event.getCommentId(), event.getArticleId(), event.getAuthor());

        if (!"COMMENT_CREATED".equals(event.getEventType())) {
            log.warn("未知的评论事件类型: {}", event.getEventType());
            return;
        }

        try {
            notificationService.createCommentNotification(event);
            log.debug("评论通知处理完成: commentId={}", event.getCommentId());
        } catch (Exception e) {
            log.error("评论通知处理失败: commentId={}, error={}",
                    event.getCommentId(), e.getMessage(), e);
            // 不抛出异常：RocketMQ 默认重试，抛出则触发重试
            throw new RuntimeException("评论通知处理失败", e);
        }
    }
}