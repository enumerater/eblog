package com.enumerate.intelligence.mapper;

import com.enumerate.intelligence.entity.ArticleAnalysis;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

public interface AnalysisMapper {
    ArticleAnalysis findByArticleId(@Param("articleId") Long articleId);
    void insert(ArticleAnalysis analysis);
    void update(ArticleAnalysis analysis);
    void deleteByArticleId(@Param("articleId") Long articleId);
    List<Map<String, Object>> findArticlesForRecommendation(@Param("articleId") Long articleId);
    List<Map<String, Object>> findAllArticleIdsAndTags();
}
