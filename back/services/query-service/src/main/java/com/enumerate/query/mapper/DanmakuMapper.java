package com.enumerate.query.mapper;

import com.enumerate.query.entity.Danmaku;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DanmakuMapper {

    void insert(Danmaku danmaku);

    /**
     * 查询某篇文章的弹幕（按时间正序）
     */
    List<Danmaku> findByArticleId(@Param("articleId") Long articleId,
                                  @Param("limit") int limit);

    /**
     * 增量拉取: 获取某个 ID 之后的弹幕
     */
    List<Danmaku> findAfterId(@Param("articleId") Long articleId,
                              @Param("afterId") Long afterId,
                              @Param("limit") int limit);
}