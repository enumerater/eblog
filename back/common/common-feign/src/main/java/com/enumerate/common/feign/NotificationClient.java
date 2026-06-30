package com.enumerate.common.feign;

import com.enumerate.common.core.result.Result;
import com.enumerate.common.feign.fallback.NotificationClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Notification Service Feign 客户端
 * 由 CommentService 在创建评论时同步调用，推送评论通知
 * 调用失败时 CommentService 会降级到 MQ 异步通知
 */
@FeignClient(
        name = "notification-service",
        path = "/api/notifications/internal",
        fallbackFactory = NotificationClientFallbackFactory.class
)
public interface NotificationClient {

    /**
     * 创建评论通知（参与 Seata 全局事务）
     */
    @PostMapping("/comment-notification")
    Result<Void> createCommentNotification(
            @RequestParam("articleId") Long articleId,
            @RequestParam("articleTitle") String articleTitle,
            @RequestParam("author") String author,
            @RequestParam("content") String content,
            @RequestParam("commentId") Long commentId,
            @RequestParam("userId") Long userId
    );
}