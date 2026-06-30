package com.enumerate.common.feign.fallback;

import com.enumerate.common.core.result.Result;
import com.enumerate.common.core.result.ResultCode;
import com.enumerate.common.feign.NotificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * NotificationClient 熔断降级工厂
 * 当 Notification Service 不可用时，返回降级结果
 * CommentService 收到降级后会尝试 MQ 异步通知
 */
@Slf4j
@Component
public class NotificationClientFallbackFactory implements FallbackFactory<NotificationClient> {

    @Override
    public NotificationClient create(Throwable cause) {
        log.error("NotificationClient 调用失败, 触发熔断降级: {}", cause.getMessage());
        return new NotificationClient() {
            @Override
            public Result<Void> createCommentNotification(Long articleId, String articleTitle, String author, String content, Long commentId, Long userId) {
                return Result.fail(ResultCode.SERVICE_UNAVAILABLE.getCode(), "通知服务暂不可用");
            }
        };
    }
}