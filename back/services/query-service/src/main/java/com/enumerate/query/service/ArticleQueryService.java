package com.enumerate.query.service;

import com.enumerate.query.dto.*;
import com.enumerate.query.entity.Article;
import com.enumerate.query.mapper.ArticleQueryMapper;
import com.enumerate.common.core.exception.BizException;
import com.enumerate.common.core.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleQueryService {

    private final ArticleQueryMapper articleQueryMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String VIEW_COUNT_KEY = "article:view:";
    private static final String HOT_ARTICLES_KEY = "article:hot";

    public PageResult<ArticleVO> getArticlePage(int page, int size, String tag) {
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 10;

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

        return PageResult.of(items, page, size, total);
    }

    public ArticleDetailVO getArticleDetail(Long id) {
        Article article = articleQueryMapper.findById(id);
        if (article == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "文章不存在");
        }
        // 异步增加浏览量
        incrementViewCount(id);
        return ArticleDetailVO.from(article, getViewCount(id) + 1);
    }

    public List<TagCountVO> getTags() {
        List<Article> all = articleQueryMapper.findAll();
        Map<String, Integer> tagCount = new HashMap<>();
        for (Article article : all) {
            List<String> tags = article.getTags();
            for (String tag : tags) {
                tagCount.merge(tag.trim().toLowerCase(), 1, Integer::sum);
            }
        }
        return tagCount.entrySet().stream()
                .map(e -> TagCountVO.builder().tag(e.getKey()).count(e.getValue()).build())
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
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
        return count != null ? Long.parseLong(count) : 0L;
    }

    private void incrementViewCount(Long articleId) {
        stringRedisTemplate.opsForValue().increment(VIEW_COUNT_KEY + articleId);
        stringRedisTemplate.opsForZSet().incrementScore(HOT_ARTICLES_KEY, String.valueOf(articleId), 1);
    }
}