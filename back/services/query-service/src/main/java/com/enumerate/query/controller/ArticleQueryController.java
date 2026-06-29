package com.enumerate.query.controller;

import com.enumerate.common.core.result.Result;
import com.enumerate.query.dto.*;
import com.enumerate.query.service.ArticleQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles-query")
@RequiredArgsConstructor
public class ArticleQueryController {

    private final ArticleQueryService articleQueryService;

    @GetMapping
    public Result<PageResult<ArticleVO>> getArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tag) {
        return Result.success(articleQueryService.getArticlePage(page, size, tag));
    }

    @GetMapping("/{id}")
    public Result<ArticleDetailVO> getArticle(@PathVariable Long id) {
        return Result.success(articleQueryService.getArticleDetail(id));
    }

    @GetMapping("/tags")
    public Result<List<TagCountVO>> getTags() {
        return Result.success(articleQueryService.getTags());
    }

    @GetMapping("/hot")
    public Result<List<ArticleVO>> getHotArticles(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(articleQueryService.getHotArticles(limit));
    }
}