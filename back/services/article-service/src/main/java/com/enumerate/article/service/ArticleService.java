package com.enumerate.article.Service;

import com.enumerate.article.Entity.Article;
import com.enumerate.article.Mapper.ArticleMapper;
import com.enumerate.article.Mapper.CommentMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleService {

    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;

    public ArticleService(ArticleMapper articleMapper, CommentMapper commentMapper) {
        this.articleMapper = articleMapper;
        this.commentMapper = commentMapper;
    }

    public List<Article> findAll() {
        return articleMapper.findAll();
    }

    public List<Article> search(String keyword, String tag) {
        return articleMapper.search(keyword, tag);
    }

    public Article findById(Long id) {
        return articleMapper.findById(id);
    }

    public Article save(Article article) {
        if (article.getSummary() == null || article.getSummary().isBlank()) {
            article.setSummary(Article.generateSummary(article.getContent()));
        }
        articleMapper.insert(article);
        return articleMapper.findById(article.getId());
    }

    public Article update(Long id, Article partial) {
        Article existing = articleMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("文章不存在");
        }

        Article toUpdate = new Article();
        toUpdate.setId(id);
        if (partial.getTitle() != null) toUpdate.setTitle(partial.getTitle());
        if (partial.getContent() != null) toUpdate.setContent(partial.getContent());
        if (partial.getTagsJson() != null) toUpdate.setTagsJson(partial.getTagsJson());
        if (partial.getSummary() != null) toUpdate.setSummary(partial.getSummary());

        articleMapper.update(toUpdate);
        return articleMapper.findById(id);
    }

    public void delete(Long id) {
        if (articleMapper.findById(id) == null) {
            throw new RuntimeException("文章不存在");
        }
        commentMapper.deleteByArticleId(id);
        articleMapper.deleteById(id);
    }
}
