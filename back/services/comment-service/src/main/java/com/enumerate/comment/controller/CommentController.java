package com.enumerate.comment.controller;

import com.enumerate.common.core.constant.CommonConstants;
import com.enumerate.common.core.result.Result;
import com.enumerate.comment.dto.CommentVO;
import com.enumerate.comment.dto.CreateCommentRequest;
import com.enumerate.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/article/{articleId}")
    public Result<List<CommentVO>> getArticleComments(@PathVariable Long articleId) {
        return Result.success(commentService.getArticleComments(articleId));
    }

    @PostMapping
    public Result<CommentVO> createComment(
            @Valid @RequestBody CreateCommentRequest request,
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId) {
        return Result.success(commentService.createComment(request, userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(
            @PathVariable Long id,
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId,
            @RequestHeader(value = CommonConstants.HEADER_USER_ROLE, required = false) String role) {
        commentService.deleteComment(id, userId, role);
        return Result.success();
    }

    @GetMapping("/count/{articleId}")
    public Result<Long> getCommentCount(@PathVariable Long articleId) {
        return Result.success(commentService.getCommentCount(articleId));
    }

    // ─── 管理后台接口 ───

    @GetMapping("/admin/list")
    public Result<List<CommentVO>> getAdminComments(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        // 简化的管理列表 - 实际项目需要完整分页
        return Result.success(List.of());
    }

    @PutMapping("/admin/{id}/approve")
    public Result<Void> approveComment(@PathVariable Long id) {
        commentService.updateStatus(id, "APPROVED");
        return Result.success();
    }

    @PutMapping("/admin/{id}/reject")
    public Result<Void> rejectComment(@PathVariable Long id) {
        commentService.updateStatus(id, "REJECTED");
        return Result.success();
    }
}