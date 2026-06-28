package com.enumerate.article.service;

import com.enumerate.article.entity.Comment;
import com.enumerate.article.mapper.CommentMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private final CommentMapper commentMapper;

    public CommentService(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    public List<Comment> findByArticleId(Long articleId) {
        return commentMapper.findByArticleId(articleId);
    }

    public Comment save(Comment comment) {
        commentMapper.insert(comment);
        return comment;
    }

    public void deleteById(Long id) {
        commentMapper.deleteById(id);
    }

    public void deleteByArticleId(Long articleId) {
        commentMapper.deleteByArticleId(articleId);
    }
}