package com.enumerate.comment.service;

import com.enumerate.comment.dto.CommentVO;
import com.enumerate.comment.dto.CreateCommentRequest;
import com.enumerate.comment.entity.Comment;
import com.enumerate.comment.mapper.CommentMapper;
import com.enumerate.common.core.constant.CommonConstants;
import com.enumerate.common.core.exception.BizException;
import com.enumerate.common.core.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;

    public List<CommentVO> getArticleComments(Long articleId) {
        List<Comment> topLevel = commentMapper.findByArticleId(articleId);
        List<CommentVO> result = new ArrayList<>();

        for (Comment comment : topLevel) {
            CommentVO vo = CommentVO.from(comment);
            // 查找回复
            List<Comment> replies = commentMapper.findByParentId(comment.getId());
            vo.setReplies(replies.stream().map(CommentVO::from).collect(Collectors.toList()));
            result.add(vo);
        }
        return result;
    }

    @Transactional
    public CommentVO createComment(CreateCommentRequest request, Long userId) {
        Comment comment = new Comment();
        comment.setArticleId(request.getArticleId());
        comment.setParentId(request.getParentId());
        comment.setAuthor(request.getAuthor());
        comment.setContent(request.getContent());
        comment.setUserId(userId);
        comment.setAvatarUrl(request.getAvatarUrl());

        // 验证父评论存在
        if (request.getParentId() != null) {
            Comment parent = commentMapper.findById(request.getParentId());
            if (parent == null) {
                throw new BizException(ResultCode.NOT_FOUND.getCode(), "回复的评论不存在");
            }
        }

        commentMapper.insert(comment);
        log.info("评论已创建: articleId={}, author={}", request.getArticleId(), request.getAuthor());
        return CommentVO.from(comment);
    }

    @Transactional
    public void deleteComment(Long id, Long userId, String role) {
        Comment comment = commentMapper.findById(id);
        if (comment == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "评论不存在");
        }
        // 仅管理员或评论者本人可删除
        boolean isOwner = userId != null && userId.equals(comment.getUserId());
        boolean isAdmin = "admin".equals(role);
        if (!isOwner && !isAdmin) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权删除该评论");
        }
        commentMapper.deleteById(id);
        log.info("评论已删除: id={}", id);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        Comment comment = commentMapper.findById(id);
        if (comment == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "评论不存在");
        }
        commentMapper.updateStatus(id, status);
        log.info("评论状态已更新: id={}, status={}", id, status);
    }

    public long getCommentCount(Long articleId) {
        return commentMapper.countByArticleId(articleId);
    }
}