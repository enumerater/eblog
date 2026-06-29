package com.enumerate.notification.controller;

import com.enumerate.common.core.constant.CommonConstants;
import com.enumerate.common.core.result.Result;
import com.enumerate.notification.dto.NotificationVO;
import com.enumerate.notification.dto.SendNotificationRequest;
import com.enumerate.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Result<List<NotificationVO>> getNotifications(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(notificationService.getNotifications(userId, page, size));
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId) {
        return Result.success(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @PathVariable Long id) {
        notificationService.markAsRead(id, userId);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId) {
        notificationService.markAllAsRead(userId);
        return Result.success();
    }

    @PostMapping("/admin")
    public Result<Void> sendNotification(
            @RequestHeader(CommonConstants.HEADER_USER_ROLE) String role,
            @Valid @RequestBody SendNotificationRequest request) {
        if (!"admin".equals(role)) {
            return Result.fail(403, "仅管理员可发送通知");
        }
        notificationService.sendNotification(request);
        return Result.success();
    }
}