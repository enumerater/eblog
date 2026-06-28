package com.enumerate.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存管理器
 * Level 1: Caffeine (本地堆内缓存)    — 极低延迟, 无网络开销
 * Level 2: Redis    (分布式缓存)      — 多服务共享, 容量大
 *
 * 读取策略: L1 → L2 → DB (由调用方实现 DB 兜底)
 * 写入策略: 同时写入 L1 + L2
 * 失效策略: 失效 L1 → 失效 L2 (通过 Redis Pub/Sub 广播通知)
 */
@Slf4j
@Component
public class MultiCacheManager {

    private Cache<String, Object> localCache;

    @Value("${cache.caffeine.max-size:5000}")
    private int caffeineMaxSize;

    @Value("${cache.caffeine.expire-minutes:30}")
    private int caffeineExpireMinutes;

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${cache.redis.default-ttl-minutes:60}")
    private int redisDefaultTtlMinutes;

    public MultiCacheManager(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        this.localCache = Caffeine.newBuilder()
                .maximumSize(caffeineMaxSize)
                .expireAfterWrite(caffeineExpireMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
        log.info("Caffeine 本地缓存初始化: maxSize={}, expire={}min", caffeineMaxSize, caffeineExpireMinutes);
    }

    // ─────────────────── 读取 ───────────────────

    /**
     * 从多级缓存读取（L1 → L2）
     * @return 缓存值, 未命中返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        // 1. 查 L1 (Caffeine)
        Object value = localCache.getIfPresent(key);
        if (value != null) {
            log.debug("L1 缓存命中: key={}", key);
            return (T) value;
        }

        // 2. 查 L2 (Redis)
        try {
            value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("L2 缓存命中: key={}", key);
                // 回写 L1 (异步)
                localCache.put(key, value);
                return (T) value;
            }
        } catch (Exception e) {
            log.warn("Redis 读取异常, 降级跳过 L2: key={}", key, e);
        }

        log.debug("缓存未命中: key={}", key);
        return null;
    }

    // ─────────────────── 写入 ───────────────────

    /**
     * 写入多级缓存
     */
    public void set(String key, Object value) {
        localCache.put(key, value);
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(redisDefaultTtlMinutes));
        } catch (Exception e) {
            log.warn("Redis 写入异常: key={}", key, e);
        }
    }

    /**
     * 写入多级缓存，指定 TTL
     */
    public void set(String key, Object value, long ttlMinutes) {
        localCache.put(key, value);
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(ttlMinutes));
        } catch (Exception e) {
            log.warn("Redis 写入异常: key={}", key, e);
        }
    }

    // ─────────────────── 失效 ───────────────────

    /**
     * 失效多级缓存
     */
    public void evict(String key) {
        localCache.invalidate(key);
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis 删除异常: key={}", key, e);
        }
    }

    /**
     * 清空本地缓存
     */
    public void clearLocal() {
        localCache.invalidateAll();
        log.info("L1 本地缓存已全部清空");
    }

    /**
     * 获取 Caffeine 统计信息
     */
    public String getStats() {
        var stats = localCache.stats();
        return String.format(
                "命中率: %.2f%%, 命中: %d, 未命中: %d, 加载成功: %d, 加载失败: %d",
                stats.hitRate() * 100,
                stats.hitCount(),
                stats.missCount(),
                stats.loadSuccessCount(),
                stats.loadFailureCount()
        );
    }
}