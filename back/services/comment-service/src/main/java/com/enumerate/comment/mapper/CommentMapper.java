package com.enumerate.comment.mapper;

import com.enumerate.comment.entity.Comment;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface CommentMapper {
    Comment findById(@Param("id") Long id);
    List<Comment> findByArticleId(@Param("articleId") Long articleId);
    List<Comment> findAllByArticleId(@Param("articleId") Long articleId);
    List<Comment> findByParentId(@Param("parentId") Long parentId);
    List<Comment> findAll(@Param("offset") int offset, @Param("limit") int limit,
                          @Param("status") String status);
    long countAll(@Param("status") String status);
    void insert(Comment comment);
    void updateStatus(@Param("id") Long id, @Param("status") String status);
    void deleteById(@Param("id") Long id);
    long countByArticleId(@Param("articleId") Long articleId);
}