package com.enumerate.intelligence.mq;

import com.enumerate.common.dto.ArticleEventDTO;
import com.enumerate.intelligence.mapper.AnalysisMapper;
import com.enumerate.intelligence.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消费者 — 文章事件
 *
 * 监听 article-events topic，消费 article-service 推送的文章创建/更新/删除事件
 * 异步执行智能分析（摘要、关键词、字数统计、阅读时长估算）
 *
 * 消费链路:
 *   article-service (publish) → MQ → intelligence-service (consume)
 *                                       ├─ ARTICLE_CREATED → 生成摘要+关键词+分析
 *                                       ├─ ARTICLE_UPDATED → 重新生成分析
 *                                       └─ ARTICLE_DELETED → 清理分析记录
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "article-events",
        consumerGroup = "intelligence-analysis-consumer"
)
public class ArticleEventConsumer implements RocketMQListener<ArticleEventDTO> {

    private final AnalysisService analysisService;
    private final AnalysisMapper analysisMapper;

    @Override
    public void onMessage(ArticleEventDTO event) {
        log.info("收到文章事件: type={}, articleId={}, title={}",
                event.getEventType(), event.getArticleId(), event.getTitle());

        try {
            switch (event.getEventType()) {
                case "ARTICLE_CREATED":
                case "ARTICLE_UPDATED":
                    handleArticleCreatedOrUpdated(event);
                    break;
                case "ARTICLE_DELETED":
                    handleArticleDeleted(event);
                    break;
                default:
                    log.warn("未知的文章事件类型: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("文章事件处理失败: type={}, articleId={}, error={}",
                    event.getEventType(), event.getArticleId(), e.getMessage(), e);
            throw new RuntimeException("文章事件处理失败", e);
        }
    }

    /** 处理文章创建/更新：异步生成摘要、关键词、统计信息 */
    private void handleArticleCreatedOrUpdated(ArticleEventDTO event) {
        Long articleId = event.getArticleId();

        // 1. 生成摘要（缓存到 DB）
        String summary = analysisService.generateSummary(articleId);
        log.debug("文章摘要已生成: articleId={}, summaryLen={}", articleId, summary.length());

        // 2. 生成完整统计数据（字数、阅读时长、关键词）
        var stats = analysisService.getArticleStats(articleId);
        log.info("文章分析完成: articleId={}, wordCount={}, readingTime={}min, keywords={}",
                articleId, stats.getWordCount(), stats.getReadingTimeMinutes(), stats.getKeywords());
    }

    /** 处理文章删除：清理分析记录 */
    private void handleArticleDeleted(ArticleEventDTO event) {
        Long articleId = event.getArticleId();
        analysisMapper.deleteByArticleId(articleId);
        log.info("文章分析记录已清理: articleId={}", articleId);
    }
}