package com.enumerate.query.mapper;

import com.enumerate.query.entity.Article;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ArticleQueryMapper {
    Article findById(@Param("id") Long id);
    List<Article> findPage(@Param("offset") int offset, @Param("limit") int limit);
    long countAll();
    List<Article> searchByTag(@Param("tag") String tag, @Param("offset") int offset, @Param("limit") int limit);
    long countByTag(@Param("tag") String tag);
    List<Article> findAll();
}