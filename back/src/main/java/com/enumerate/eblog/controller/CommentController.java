package com.enumerate.eblog.controller;

import com.enumerate.eblog.entity.Comment;
import com.enumerate.eblog.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles/{articleId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<Comment> getAll(@PathVariable Long articleId) {
        return commentService.findByArticleId(articleId);
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long articleId, @RequestBody Map<String, String> body) {
        String author = body.get("author");
        String content = body.get("content");

        if (author == null || author.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "昵称不能为空"));
        }
        if (content == null || content.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "评论内容不能为空"));
        }

        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setAuthor(author.trim());
        comment.setContent(content.trim());

        Comment saved = commentService.save(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long articleId, @PathVariable Long id) {
        commentService.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }
}