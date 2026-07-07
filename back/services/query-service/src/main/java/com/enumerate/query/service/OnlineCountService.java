package com.enumerate.query.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 实时在线人数统计 (基于 Redis ZSet)
 *
 * 思路:
 *   - 每个访客生成一个唯一 sessionId (存 localStorage)
 *   - 前端每 60s 调用 heartbeat(sessionId)
 *   - Redis ZSet `online:visitors` 中 member=sessionId, score=最后心跳时间戳
 *   - 定时任务每 30s 清理超过 3 分钟无心跳的 session
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineCountService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String ONLINE_KEY = "online:visitors";
    private static final long HEARTBEAT_TIMEOUT_SECONDS = 180; // 3 分钟超时
    private static final long CLEANUP_INTERVAL_MS = 30_000;    // 30s 清理一次

    /**
     * 记录心跳
     * @param sessionId 访客唯一标识 (前端生成)
     * @param request   用于获取 IP (可选记录)
     */
    public void heartbeat(String sessionId, HttpServletRequest request) {
        long now = System.currentTimeMillis();
        stringRedisTemplate.opsForZSet().add(ONLINE_KEY, sessionId, now);
        // 设置 key 整体过期时间防止内存泄漏 (不过期太短，清理任务负责移除)
        stringRedisTemplate.expire(ONLINE_KEY, 1, TimeUnit.DAYS);
    }

    /**
     * 获取当前在线人数
     */
    public long getOnlineCount() {
        long threshold = System.currentTimeMillis() - (HEARTBEAT_TIMEOUT_SECONDS * 1000);
        // 只统计在超时时间内的 session
        Long count = stringRedisTemplate.opsForZSet().count(ONLINE_KEY, threshold, Double.MAX_VALUE);
        return count != null ? count : 0L;
    }

    /**
     * 生成一个新的 sessionId
     */
    public static String generateSessionId() {
        return "anon:" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 定时清理过期 session (每 30s)
     */
    @Scheduled(fixedRate = CLEANUP_INTERVAL_MS)
    public void cleanupExpiredSessions() {
        long threshold = System.currentTimeMillis() - (HEARTBEAT_TIMEOUT_SECONDS * 1000);
        Long removed = stringRedisTemplate.opsForZSet().removeRangeByScore(ONLINE_KEY, 0, threshold);
        if (removed != null && removed > 0) {
            log.debug("在线心跳清理: 移除 {} 个过期 session", removed);
        }
    }
}