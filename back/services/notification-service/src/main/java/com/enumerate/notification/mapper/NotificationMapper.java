package com.enumerate.notification.mapper;

import com.enumerate.notification.entity.Notification;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface NotificationMapper {
    List<Notification> findByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
    long countByUserId(@Param("userId") Long userId);
    long countUnreadByUserId(@Param("userId") Long userId);
    Notification findById(@Param("id") Long id);
    void insert(Notification notification);
    void markAsRead(@Param("id") Long id, @Param("userId") Long userId);
    void markAllAsRead(@Param("userId") Long userId);
}