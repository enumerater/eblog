package com.enumerate.query.entity;

import java.time.LocalDateTime;

/**
 * 弹幕实体
 */
public class Danmaku {

    private Long id;
    private Long articleId;
    private String content;
    private String author;
    private String color;
    private Integer position; // 0=滚动, 1=顶部, 2=底部
    private String ip;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}