package com.enumerate.article.mapper;

import com.enumerate.article.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {

    List<Comment> findByArticleId(@Param("articleId") Long articleId);

    int insert(Comment comment);

    int deleteById(@Param("id") Long id);

    int deleteByArticleId(@Param("articleId") Long articleId);
}