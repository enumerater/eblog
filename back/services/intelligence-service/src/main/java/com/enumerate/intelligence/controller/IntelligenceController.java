package com.enumerate.intelligence.controller;

import com.enumerate.common.core.exception.BizException;
import com.enumerate.common.core.result.Result;
import com.enumerate.intelligence.dto.ArticleStatsVO;
import com.enumerate.intelligence.dto.RecommendationVO;
import com.enumerate.intelligence.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intelligence")
@RequiredArgsConstructor
public class IntelligenceController {

    private final AnalysisService analysisService;

    @GetMapping("/articles/{id}/summary")
    public Result<String> getArticleSummary(@PathVariable Long id) {
        return Result.success(analysisService.generateSummary(id));
    }

    @GetMapping("/articles/{id}/stats")
    public Result<ArticleStatsVO> getArticleStats(@PathVariable Long id) {
        return Result.success(analysisService.getArticleStats(id));
    }

    @GetMapping("/recommendations/{articleId}")
    public Result<List<RecommendationVO>> getRecommendations(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "5") int limit) {
        return Result.success(analysisService.getRecommendations(articleId, limit));
    }

    @PostMapping("/tags/suggest")
    public Result<List<String>> suggestTags(@RequestBody Map<String, Object> request) {
        Long articleId = request.get("articleId") != null
                ? Long.valueOf(request.get("articleId").toString()) : null;
        String content = (String) request.get("content");
        if (articleId == null && (content == null || content.isBlank())) {
            return Result.fail(400, "请提供文章ID或内容");
        }
        return Result.success(analysisService.suggestTags(articleId, content));
    }
}
