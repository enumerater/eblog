package com.enumerate.query.service;

import com.enumerate.common.cache.MultiCacheManager;
import com.enumerate.common.cache.BloomFilterManager;
import com.enumerate.query.dto.*;
import com.enumerate.query.entity.Article;
import com.enumerate.query.mapper.ArticleQueryMapper;
import com.enumerate.common.core.exception.BizException;
import com.enumerate.common.core.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleQueryService {

    private final ArticleQueryMapper articleQueryMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final MultiCacheManager cacheManager;
    private final BloomFilterManager bloomFilterManager;

    // ─── 缓存 Key 常量 ───
    private static final String CACHE_DETAIL = "cache:article:detail:";
    private static final String CACHE_PAGE = "cache:article:page:";
    private static final String CACHE_TAGS = "cache:article:tags";
    private static final String PREFIX_CACHE_EVICT = "cache:article:";

    private static final String VIEW_COUNT_KEY = "article:view:";
    private static final String HOT_ARTICLES_KEY = "article:hot";

    // ══════════════════════════════════════════════
    // 定时任务: 每 5 分钟将 Redis 浏览量刷到 MySQL
    // ══════════════════════════════════════════════

    /**
     * 将 Redis 中累积的增量浏览量刷到 DB
     * Redis 中的值为 (DB基础值 + 增量)，直接回写保持同步
     */
    @Scheduled(fixedRate = 300_000) // 5分钟
    public void flushViewCounts() {
        Set<String> keys = stringRedisTemplate.keys(VIEW_COUNT_KEY + "*");
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            try {
                Long articleId = Long.parseLong(key.substring(VIEW_COUNT_KEY.length()));
                String countStr = stringRedisTemplate.opsForValue().get(key);
                if (countStr != null) {
                    long count = Long.parseLong(countStr);
                    articleQueryMapper.updateViewCount(articleId, count);
                }
            } catch (Exception e) {
                log.warn("刷浏览量失败: key={}", key, e);
            }
        }
        log.debug("浏览量刷库完成, 共 {} 篇", keys.size());
    }

    public PageResult<ArticleVO> getArticlePage(int page, int size, String tag) {
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 10;

        // ── 缓存 Key: 按分页参数 + 标签过滤维度 ──
        String cacheKey = CACHE_PAGE + page + ":" + size + ":" + (tag != null ? tag : "");
        PageResult<ArticleVO> cached = cacheManager.get(cacheKey, PageResult.class);
        if (cached != null) {
            return cached;
        }

        int offset = (page - 1) * size;
        List<Article> articles;
        long total;

        if (tag != null && !tag.isBlank()) {
            articles = articleQueryMapper.searchByTag(tag, offset, size);
            total = articleQueryMapper.countByTag(tag);
        } else {
            articles = articleQueryMapper.findPage(offset, size);
            total = articleQueryMapper.countAll();
        }

        List<ArticleVO> items = articles.stream()
                .map(a -> ArticleVO.from(a, getViewCount(a.getId())))
                .collect(Collectors.toList());

        PageResult<ArticleVO> result = PageResult.of(items, page, size, total);

        // 分页缓存 TTL 较短（5 分钟），保证新文章能相对快地被看到
        cacheManager.set(cacheKey, result, 5);
        log.debug("分页缓存已写入: key={}, hitRate={}", cacheKey, cacheManager.getStats());

        return result;
    }

    public ArticleDetailVO getArticleDetail(Long id) {
        // ── L0: 布隆过滤器 — 判断 ID 是否存在 (防缓存穿透) ──
        if (!bloomFilterManager.mightContain(id)) {
            log.info("布隆过滤器拦截不存在文章: id={}", id);
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "文章不存在");
        }

        // ── L1/L2 缓存读取 ──
        String cacheKey = CACHE_DETAIL + id;
        ArticleDetailVO cached = cacheManager.get(cacheKey, ArticleDetailVO.class);
        if (cached != null) {
            // 缓存命中也递增阅读计数（异步，不影响响应）
            incrementViewCount(id);
            // 返回时从 Redis 获取最新计数，覆盖缓存中的值
            Long redisCount = getViewCount(id);
            if (redisCount != null && redisCount > 0) {
                cached.setViewCount(redisCount);
            }
            return cached;
        }

        Article article = articleQueryMapper.findById(id);
        if (article == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "文章不存在");
        }
        incrementViewCount(id);
        // 以 DB 中的 view_count 为基础
        long dbCount = article.getViewCount() != null ? article.getViewCount() : 0L;
        long redisDelta = getViewCount(id) != null ? getViewCount(id) - dbCount : 0L;
        long totalViewCount = Math.max(dbCount, getViewCount(id) != null ? getViewCount(id) : dbCount);

        ArticleDetailVO vo = ArticleDetailVO.from(article, totalViewCount);
        // 文章详情缓存 30 分钟，内容变更通过 MQ 广播 evict（参见 article-events 消费者）
        cacheManager.set(cacheKey, vo, 30);
        log.debug("文章详情缓存已写入: key={}, hitRate={}", cacheKey, cacheManager.getStats());
        return vo;
    }

    public List<TagCountVO> getTags() {
        // ── 缓存读取 ──
        List<TagCountVO> cached = cacheManager.get(CACHE_TAGS, List.class);
        if (cached != null) {
            return cached;
        }

        List<Article> all = articleQueryMapper.findAll();
        Map<String, Integer> tagCount = new HashMap<>();
        for (Article article : all) {
            List<String> tags = article.getTags();
            for (String tag : tags) {
                tagCount.merge(tag.trim().toLowerCase(), 1, Integer::sum);
            }
        }
        List<TagCountVO> result = tagCount.entrySet().stream()
                .map(e -> TagCountVO.builder().tag(e.getKey()).count(e.getValue()).build())
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());

        cacheManager.set(CACHE_TAGS, result, 30);
        log.debug("标签缓存已写入, 数量={}, hitRate={}", result.size(), cacheManager.getStats());
        return result;
    }

    public List<ArticleVO> getHotArticles(int limit) {
        String key = HOT_ARTICLES_KEY;
        Set<String> hotIds = stringRedisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);
        if (hotIds == null || hotIds.isEmpty()) return List.of();

        List<ArticleVO> result = new ArrayList<>();
        for (String idStr : hotIds) {
            Article article = articleQueryMapper.findById(Long.parseLong(idStr));
            if (article != null) {
                Long viewCount = getViewCount(article.getId());
                result.add(ArticleVO.from(article, viewCount));
            }
        }
        return result;
    }

    private Long getViewCount(Long articleId) {
        String count = stringRedisTemplate.opsForValue().get(VIEW_COUNT_KEY + articleId);
        if (count != null) {
            return Long.parseLong(count);
        }
        // Redis 中无记录时，从 DB 读取作为初始值
        Article article = articleQueryMapper.findById(articleId);
        if (article != null && article.getViewCount() != null) {
            stringRedisTemplate.opsForValue().set(VIEW_COUNT_KEY + articleId, String.valueOf(article.getViewCount()));
            return article.getViewCount();
        }
        return 0L;
    }

    private void incrementViewCount(Long articleId) {
        stringRedisTemplate.opsForValue().increment(VIEW_COUNT_KEY + articleId);
        stringRedisTemplate.opsForZSet().incrementScore(HOT_ARTICLES_KEY, String.valueOf(articleId), 1);
    }

    // ─────────────────── 缓存管理（供 MQ 消费者 / 管理接口调用） ───────────────────

    /**
     * 文章变更时清除相关缓存
     * 由 article-events 消费者在收到 ARTICLE_UPDATED / ARTICLE_DELETED 时调用
     */
    public void evictArticleCache(Long articleId) {
        // 1. 失效文章详情
        String detailKey = CACHE_DETAIL + articleId;
        cacheManager.evict(detailKey);
        log.info("缓存 evict: {}", detailKey);

        // 2. 失效标签聚合（因为文章变化影响标签计数）
        cacheManager.evict(CACHE_TAGS);
        log.info("缓存 evict: {}", CACHE_TAGS);

        // 3. 分页缓存无法精确找到某一页，保留 TTL 自动过期
        //    生产环境可扩展为缓存 Key 扫描或版本号机制
    }

    /**
     * 获取缓存命中率统计
     */
    public String getCacheStats() {
        return cacheManager.getStats();
    }
}