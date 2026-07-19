package com.enumerate.eblog.mapper;

import com.enumerate.eblog.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArticleMapper {

    List<Article> findAll();

    List<Article> findAllWithPagination(@Param("offset") int offset, @Param("limit") int limit);

    long countAll();

    List<Article> search(@Param("keyword") String keyword, @Param("tag") String tag);

    List<Article> searchWithPagination(@Param("keyword") String keyword, @Param("tag") String tag,
                                       @Param("offset") int offset, @Param("limit") int limit);

    long countSearch(@Param("keyword") String keyword, @Param("tag") String tag);

    Article findById(@Param("id") Long id);

    int insert(Article article);

    int update(Article article);

    int deleteById(@Param("id") Long id);
}