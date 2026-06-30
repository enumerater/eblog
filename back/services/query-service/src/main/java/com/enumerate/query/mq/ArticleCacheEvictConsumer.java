package com.enumerate.query.mq;

import com.enumerate.common.dto.ArticleEventDTO;
import com.enumerate.query.service.ArticleQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消费者 — 文章缓存失效广播
 *
 * 监听 article-events topic，
 * 当文章发生变更（创建/更新/删除）时，失效 query-service 中的相关缓存
 *
 * 消费链路:
 *   article-service (publish) → MQ → query-service (consume)
 *                                       ├─ ARTICLE_CREATED → 失效标签+分页缓存
 *                                       ├─ ARTICLE_UPDATED → 失效详情+标签+分页缓存
 *                                       └─ ARTICLE_DELETED → 失效详情+标签+分页缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "article-events",
        consumerGroup = "query-cache-evict-consumer"
)
public class ArticleCacheEvictConsumer implements RocketMQListener<ArticleEventDTO> {

    private final ArticleQueryService articleQueryService;

    @Override
    public void onMessage(ArticleEventDTO event) {
        log.info("收到缓存失效事件: type={}, articleId={}", event.getEventType(), event.getArticleId());

        try {
            articleQueryService.evictArticleCache(event.getArticleId());
            log.info("缓存失效完成: articleId={}, cacheStats={}",
                    event.getArticleId(), articleQueryService.getCacheStats());
        } catch (Exception e) {
            log.error("缓存失效失败: articleId={}, error={}",
                    event.getArticleId(), e.getMessage(), e);
            throw new RuntimeException("缓存失效失败", e);
        }
    }
}