package com.enumerate.search.mq;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.enumerate.common.dto.ArticleEventDTO;
import com.enumerate.search.document.ArticleDocument;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * RocketMQ 消费者 — 文章事件 → ES 索引同步
 *
 * 监听 article-events topic（与 IntelligenceService / QueryService 共用同一 Topic）
 * 消费 article-service 推送的文章创建/更新/删除事件，同步至 Elasticsearch
 *
 * ES 中只存纯文本（HTML 已剥离），确保高亮正常工作
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "article-events",
        consumerGroup = "search-index-consumer"
)
public class ArticleIndexConsumer implements RocketMQListener<ArticleEventDTO> {

    private final ElasticsearchClient esClient;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String ES_INDEX = "articles";

    @Override
    public void onMessage(ArticleEventDTO event) {
        log.info("收到文章事件: type={}, articleId={}, title={}",
                event.getEventType(), event.getArticleId(), event.getTitle());

        try {
            switch (event.getEventType()) {
                case "ARTICLE_CREATED":
                case "ARTICLE_UPDATED":
                    handleIndexOrUpdate(event);
                    break;
                case "ARTICLE_DELETED":
                    handleDelete(event);
                    break;
                default:
                    log.warn("未知文章事件类型: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("ES 索引同步失败: type={}, articleId={}, error={}",
                    event.getEventType(), event.getArticleId(), e.getMessage(), e);
            throw new RuntimeException("ES 索引同步失败", e);
        }
    }

    /** 创建/更新 ES 文档 — 只存纯文本 */
    private void handleIndexOrUpdate(ArticleEventDTO event) throws Exception {
        String now = LocalDateTime.now().format(DT_FMT);
        List<String> tags = parseTags(event.getTagsJson());

        // ES 只存纯文本, 不然高亮会嵌在 HTML 标签里
        String plainContent = stripHtml(event.getContent());

        ArticleDocument doc = ArticleDocument.builder()
                .id(event.getArticleId())
                .title(event.getTitle())
                .content(plainContent)
                .summary(generateSummary(event.getContent()))
                .tags(tags)
                .createdAt(now)
                .updatedAt(now)
                .build();

        esClient.index(i -> i
                .index(ES_INDEX)
                .id(String.valueOf(event.getArticleId()))
                .document(doc));

        log.info("ES 文档已索引 (纯文本): articleId={}, title={}", event.getArticleId(), event.getTitle());
    }

    /** 删除 ES 文档 */
    private void handleDelete(ArticleEventDTO event) throws Exception {
        esClient.delete(d -> d
                .index(ES_INDEX)
                .id(String.valueOf(event.getArticleId())));
        log.info("ES 文档已删除: articleId={}", event.getArticleId());
    }

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) return List.of();
        try {
            return MAPPER.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
    }

    private String generateSummary(String html) {
        if (html == null || html.isBlank()) return "";
        String text = stripHtml(html);
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }
}