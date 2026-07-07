package com.enumerate.query.controller;

import com.enumerate.common.core.result.Result;
import com.enumerate.query.entity.Danmaku;
import com.enumerate.query.service.DanmakuService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 弹幕 API
 */
@RestController
@RequestMapping("/api/danmaku")
@RequiredArgsConstructor
public class DanmakuController {

    private final DanmakuService danmakuService;

    /**
     * 发送弹幕
     * POST /api/danmaku
     * Body: { articleId, content, author?, color?, position? }
     */
    @PostMapping
    public Result<Danmaku> send(@RequestBody Danmaku danmaku, HttpServletRequest request) {
        try {
            Danmaku saved = danmakuService.sendDanmaku(danmaku, request);
            return Result.success(saved);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 获取文章弹幕列表（初始加载）
     * GET /api/danmaku/{articleId}?limit=200
     */
    @GetMapping("/{articleId}")
    public Result<List<Danmaku>> getDanmaku(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "200") int limit) {
        return Result.success(danmakuService.getDanmaku(articleId, limit));
    }

    /**
     * 增量拉取弹幕（轮询用）
     * GET /api/danmaku/{articleId}/after/{afterId}?limit=50
     */
    @GetMapping("/{articleId}/after/{afterId}")
    public Result<List<Danmaku>> getDanmakuAfter(
            @PathVariable Long articleId,
            @PathVariable Long afterId,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(danmakuService.getDanmakuAfter(articleId, afterId, limit));
    }
}