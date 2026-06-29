package com.enumerate.notification.service;

import com.enumerate.common.core.exception.BizException;
import com.enumerate.common.core.result.ResultCode;
import com.enumerate.notification.dto.NotificationVO;
import com.enumerate.notification.dto.SendNotificationRequest;
import com.enumerate.notification.entity.Notification;
import com.enumerate.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public List<NotificationVO> getNotifications(Long userId, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 10;
        int offset = (page - 1) * size;

        return notificationMapper.findByUserId(userId, offset, size).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        return notificationMapper.countUnreadByUserId(userId);
    }

    @Transactional
    public void markAsRead(Long id, Long userId) {
        notificationMapper.markAsRead(id, userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationMapper.markAllAsRead(userId);
    }

    @Transactional
    public void sendNotification(SendNotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId() != null ? request.getUserId() : 0L);
        notification.setType(request.getType() != null ? request.getType() : "SYSTEM");
        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setRelatedId(request.getRelatedId());
        notificationMapper.insert(notification);
        log.info("发送通知: userId={}, title={}", notification.getUserId(), notification.getTitle());
    }

    private NotificationVO toVO(Notification n) {
        return NotificationVO.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .relatedId(n.getRelatedId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}