package com.enumerate.article.service;

import com.enumerate.article.entity.Article;
import com.enumerate.article.mapper.ArticleMapper;
import com.enumerate.article.mapper.CommentMapper;
import com.enumerate.common.dto.ArticleEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ArticleService {

    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final RocketMQTemplate rocketMQTemplate;

    private static final String ARTICLE_EVENTS_TOPIC = "article-events";

    public ArticleService(ArticleMapper articleMapper,
                          CommentMapper commentMapper,
                          RocketMQTemplate rocketMQTemplate) {
        this.articleMapper = articleMapper;
        this.commentMapper = commentMapper;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public List<Article> findAll() {
        return articleMapper.findAll();
    }

    public List<Article> search(String keyword, String tag) {
        return articleMapper.search(keyword, tag);
    }

    public Article findById(Long id) {
        return articleMapper.findById(id);
    }

    public Article save(Article article) {
        if (article.getSummary() == null || article.getSummary().isBlank()) {
            article.setSummary(Article.generateSummary(article.getContent()));
        }
        articleMapper.insert(article);
        Article saved = articleMapper.findById(article.getId());

        // 异步推送文章创建事件 → 下游服务消费（智能分析、缓存失效等）
        publishArticleEvent("ARTICLE_CREATED", saved);

        return saved;
    }

    public Article update(Long id, Article partial) {
        Article existing = articleMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("文章不存在");
        }

        Article toUpdate = new Article();
        toUpdate.setId(id);
        if (partial.getTitle() != null) toUpdate.setTitle(partial.getTitle());
        if (partial.getContent() != null) toUpdate.setContent(partial.getContent());
        if (partial.getTagsJson() != null) toUpdate.setTagsJson(partial.getTagsJson());
        if (partial.getSummary() != null) toUpdate.setSummary(partial.getSummary());

        articleMapper.update(toUpdate);
        Article updated = articleMapper.findById(id);

        // 异步推送文章更新事件
        publishArticleEvent("ARTICLE_UPDATED", updated);

        return updated;
    }

    public void delete(Long id) {
        if (articleMapper.findById(id) == null) {
            throw new RuntimeException("文章不存在");
        }
        commentMapper.deleteByArticleId(id);
        articleMapper.deleteById(id);

        // 异步推送文章删除事件（不传内容，仅传 ID）
        ArticleEventDTO event = ArticleEventDTO.builder()
                .eventType("ARTICLE_DELETED")
                .articleId(id)
                .timestamp(System.currentTimeMillis())
                .build();
        publishEventSafe(event);
    }

    // ─────────────────── MQ 事件推送 ───────────────────

    /** 构建并推送文章事件 */
    private void publishArticleEvent(String eventType, Article article) {
        ArticleEventDTO event = ArticleEventDTO.builder()
                .eventType(eventType)
                .articleId(article.getId())
                .title(article.getTitle())
                .content(article.getContent())
                .tagsJson(article.getTagsJson())
                .timestamp(System.currentTimeMillis())
                .build();
        publishEventSafe(event);
    }

    /** 发送 MQ 消息（发送失败不阻塞主流程） */
    private void publishEventSafe(ArticleEventDTO event) {
        try {
            rocketMQTemplate.convertAndSend(ARTICLE_EVENTS_TOPIC, event);
            log.debug("文章事件已推送 MQ: type={}, articleId={}",
                    event.getEventType(), event.getArticleId());
        } catch (Exception e) {
            log.warn("文章事件推送 MQ 失败, 不影响主流程: type={}, articleId={}, error={}",
                    event.getEventType(), event.getArticleId(), e.getMessage());
        }
    }
}
