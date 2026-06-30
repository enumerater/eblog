package com.enumerate.notification.controller;

import com.enumerate.common.core.result.Result;
import com.enumerate.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知服务内部接口
 * 供其他微服务通过 Feign 调用，参与 Seata 全局事务
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications/internal")
@RequiredArgsConstructor
public class NotificationInternalController {

    private final NotificationService notificationService;

    /**
     * 创建评论通知（由 CommentService 通过 Feign 调用）
     * 此方法参与 CommentService 发起的 Seata 全局事务
     */
    @PostMapping("/comment-notification")
    public Result<Void> createCommentNotification(
            @RequestParam("articleId") Long articleId,
            @RequestParam("articleTitle") String articleTitle,
            @RequestParam("author") String author,
            @RequestParam("content") String content,
            @RequestParam("commentId") Long commentId,
            @RequestParam("userId") Long userId) {
        notificationService.createCommentNotificationSync(articleId, articleTitle, author, content);
        return Result.success();
    }
}