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

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;

    /**
     * 获取文章评论，仅两层：顶级评论 + 平铺回复
     * 回复第三级及以上时，展示 "XXX 回复 @YYY"
     */
    public List<CommentVO> getArticleComments(Long articleId) {
        List<Comment> all = commentMapper.findAllByArticleId(articleId);
        if (all == null || all.isEmpty()) return List.of();

        // id → entity 用于向上追溯
        Map<Long, Comment> entityById = all.stream()
                .collect(Collectors.toMap(Comment::getId, c -> c, (a, b) -> a));

        // 顶级评论
        List<Comment> topLevel = all.stream()
                .filter(c -> c.getParentId() == null)
                .collect(Collectors.toList());

        List<CommentVO> roots = new ArrayList<>();
        Map<Long, CommentVO> rootById = new HashMap<>();
        for (Comment c : topLevel) {
            CommentVO vo = CommentVO.from(c);
            vo.setReplies(new ArrayList<>());
            roots.add(vo);
            rootById.put(vo.getId(), vo);
        }

        // 非顶级评论 — 全部平铺挂到顶级下
        List<Comment> replies = all.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.toList());

        for (Comment reply : replies) {
            CommentVO replyVO = CommentVO.from(reply);

            // 找到直接父评论的 author → replyToName
            Comment parent = entityById.get(reply.getParentId());
            if (parent == null) continue; // 父被删了，忽略

            // 除非是直接回复顶级，否则设置 replyToName
            if (parent.getParentId() != null) {
                replyVO.setReplyToName(parent.getAuthor());
            }

            // 向上找到顶级评论
            Comment top = parent;
            while (top.getParentId() != null) {
                top = entityById.get(top.getParentId());
                if (top == null) break;
            }

            if (top != null) {
                CommentVO root = rootById.get(top.getId());
                if (root != null) {
                    root.getReplies().add(replyVO);
                } else {
                    roots.add(replyVO);
                }
            } else {
                roots.add(replyVO);
            }
        }

        return roots;
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