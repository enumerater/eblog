package com.enumerate.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendNotificationRequest {
    private Long userId;       // null=广播

    @NotBlank(message = "通知标题不能为空")
    private String title;

    private String content;
    private String type;
    private Long relatedId;
}