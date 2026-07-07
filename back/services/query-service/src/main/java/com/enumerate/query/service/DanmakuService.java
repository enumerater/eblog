package com.enumerate.query.service;

import com.enumerate.query.entity.Danmaku;
import com.enumerate.query.mapper.DanmakuMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 弹幕服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DanmakuService {

    private final DanmakuMapper danmakuMapper;

    /**
     * 发送弹幕
     */
    public Danmaku sendDanmaku(Danmaku danmaku, HttpServletRequest request) {
        // 长度限制
        if (danmaku.getContent() == null || danmaku.getContent().isBlank()) {
            throw new IllegalArgumentException("弹幕内容不能为空");
        }
        if (danmaku.getContent().length() > 200) {
            danmaku.setContent(danmaku.getContent().substring(0, 200));
        }
        // 默认值
        if (danmaku.getAuthor() == null || danmaku.getAuthor().isBlank()) {
            danmaku.setAuthor("匿名");
        }
        if (danmaku.getColor() == null || danmaku.getColor().isBlank()) {
            danmaku.setColor("#ffffff");
        }
        if (danmaku.getPosition() == null) {
            danmaku.setPosition(0);
        }
        // 记录 IP
        String ip = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip)) {
            ip = "localhost";
        }
        danmaku.setIp(ip);

        danmakuMapper.insert(danmaku);
        return danmaku;
    }

    /**
     * 获取某篇文章的弹幕（初始加载，最多 200 条）
     */
    public List<Danmaku> getDanmaku(Long articleId, int limit) {
        if (limit <= 0 || limit > 500) limit = 200;
        return danmakuMapper.findByArticleId(articleId, limit);
    }

    /**
     * 增量拉取弹幕（用于轮询）
     */
    public List<Danmaku> getDanmakuAfter(Long articleId, Long afterId, int limit) {
        if (limit <= 0 || limit > 100) limit = 50;
        return danmakuMapper.findAfterId(articleId, afterId, limit);
    }
}