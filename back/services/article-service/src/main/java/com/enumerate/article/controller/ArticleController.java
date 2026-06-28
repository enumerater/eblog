package com.enumerate.article.controller;

import com.enumerate.article.dto.ArticleResponse;
import com.enumerate.article.entity.Article;
import com.enumerate.article.service.ArticleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public List<ArticleResponse> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag) {
        List<Article> articles;
        if ((keyword != null && !keyword.isBlank()) || (tag != null && !tag.isBlank())) {
            articles = articleService.search(keyword, tag);
        } else {
            articles = articleService.findAll();
        }
        return articles.stream()
                .map(ArticleResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Article article = articleService.findById(id);
        if (article == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "文章不存在"));
        }
        return ResponseEntity.ok(ArticleResponse.from(article));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");

        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "标题和内容不能为空"));
        }

        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);

        if (body.containsKey("tags")) {
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) body.get("tags");
            article.setTags(tags);
        }

        if (body.containsKey("summary")) {
            article.setSummary((String) body.get("summary"));
        }

        Article saved = articleService.save(article);
        return ResponseEntity.status(HttpStatus.CREATED).body(ArticleResponse.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Article partial = new Article();
            if (body.containsKey("title")) partial.setTitle((String) body.get("title"));
            if (body.containsKey("content")) partial.setContent((String) body.get("content"));
            if (body.containsKey("summary")) partial.setSummary((String) body.get("summary"));
            if (body.containsKey("tags")) {
                @SuppressWarnings("unchecked")
                List<String> tags = (List<String>) body.get("tags");
                partial.setTags(tags);
            }

            Article updated = articleService.update(id, partial);
            return ResponseEntity.ok(ArticleResponse.from(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "文章不存在"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            articleService.delete(id);
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "文章不存在"));
        }
    }
}
