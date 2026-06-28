package com.enumerate.article.mapper;

import com.enumerate.article.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArticleMapper {

    List<Article> findAll();

    List<Article> search(@Param("keyword") String keyword, @Param("tag") String tag);

    Article findById(@Param("id") Long id);

    int insert(Article article);

    int update(Article article);

    int deleteById(@Param("id") Long id);
}
