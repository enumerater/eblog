package com.enumerate.article.controller;

import com.enumerate.article.dto.ArticleResponse;
import com.enumerate.article.dto.DraftResponse;
import com.enumerate.article.entity.Article;
import com.enumerate.article.entity.Draft;
import com.enumerate.article.service.ArticleService;
import com.enumerate.article.service.DraftService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drafts")
public class DraftController {

    private final DraftService draftService;
    private final ArticleService articleService;

    public DraftController(DraftService draftService, ArticleService articleService) {
        this.draftService = draftService;
        this.articleService = articleService;
    }

    @GetMapping
    public List<DraftResponse> getAll() {
        return draftService.findAll().stream()
                .map(DraftResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Draft draft = draftService.findById(id);
        if (draft == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "草稿不存在"));
        }
        return ResponseEntity.ok(DraftResponse.from(draft));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String title = (String) body.getOrDefault("title", "");
        String content = (String) body.getOrDefault("content", "");

        if (title.isBlank() && content.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "标题和内容不能同时为空"));
        }

        Draft draft = new Draft();
        draft.setTitle(title);
        draft.setContent(content);

        if (body.containsKey("tags")) {
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) body.get("tags");
            draft.setTags(tags);
        }

        if (body.containsKey("summary")) {
            draft.setSummary((String) body.get("summary"));
        }

        if (body.containsKey("articleId") && body.get("articleId") != null) {
            draft.setArticleId(((Number) body.get("articleId")).longValue());
        }

        Draft saved = draftService.save(draft);
        return ResponseEntity.status(HttpStatus.CREATED).body(DraftResponse.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Draft partial = new Draft();
            if (body.containsKey("title")) partial.setTitle((String) body.get("title"));
            if (body.containsKey("content")) partial.setContent((String) body.get("content"));
            if (body.containsKey("summary")) partial.setSummary((String) body.get("summary"));
            if (body.containsKey("tags")) {
                @SuppressWarnings("unchecked")
                List<String> tags = (List<String>) body.get("tags");
                partial.setTags(tags);
            }
            if (body.containsKey("articleId") && body.get("articleId") != null) {
                partial.setArticleId(((Number) body.get("articleId")).longValue());
            }

            Draft updated = draftService.update(id, partial);
            return ResponseEntity.ok(DraftResponse.from(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "草稿不存在"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            draftService.delete(id);
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "草稿不存在"));
        }
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publish(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Draft draft = draftService.findById(id);
        if (draft == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "草稿不存在"));
        }

        String title = draft.getTitle();
        String content = draft.getContent();
        List<String> tags = draft.getTags();

        if (body != null) {
            if (body.containsKey("title")) title = (String) body.get("title");
            if (body.containsKey("content")) content = (String) body.get("content");
            if (body.containsKey("tags")) {
                @SuppressWarnings("unchecked")
                List<String> overrideTags = (List<String>) body.get("tags");
                if (overrideTags != null) tags = overrideTags;
            }
        }

        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "标题和内容不能为空"));
        }

        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);
        article.setTags(tags);

        Article saved;
        if (draft.getArticleId() != null) {
            try {
                saved = articleService.update(draft.getArticleId(), article);
            } catch (RuntimeException e) {
                saved = articleService.save(article);
            }
        } else {
            saved = articleService.save(article);
        }

        draftService.delete(id);

        return ResponseEntity.ok(ArticleResponse.from(saved));
    }
}
