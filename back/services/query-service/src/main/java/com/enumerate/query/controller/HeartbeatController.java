package com.enumerate.query.controller;

import com.enumerate.common.core.result.Result;
import com.enumerate.query.service.OnlineCountService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 实时在线人数 + 访客心跳
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class HeartbeatController {

    private final OnlineCountService onlineCountService;

    /**
     * 访客心跳
     * POST /api/analytics/heartbeat
     * Body: { "sessionId": "..." }
     */
    @PostMapping("/heartbeat")
    public Result<Void> heartbeat(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            return Result.fail("sessionId 不能为空");
        }
        onlineCountService.heartbeat(sessionId, request);
        return Result.success(null);
    }

    /**
     * 获取当前在线人数
     * GET /api/analytics/online-count
     */
    @GetMapping("/online-count")
    public Result<Long> getOnlineCount() {
        return Result.success(onlineCountService.getOnlineCount());
    }

    /**
     * 获取新的 sessionId (前端初始化时调用)
     * GET /api/analytics/session-id
     */
    @GetMapping("/session-id")
    public Result<Map<String, String>> getSessionId() {
        return Result.success(Map.of("sessionId", OnlineCountService.generateSessionId()));
    }
}