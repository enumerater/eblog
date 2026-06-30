package com.enumerate.search.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.List;

/**
 * Elasticsearch 文章文档
 *
 * 索引配置:
 *   - ik_smart:          标题/摘要 — 粗粒度分词, 精度高
 *   - ik_max_word:       内容 — 细粒度分词, 提高召回率
 *   - tags:              Keyword, 精确匹配 + 聚合
 *
 * IK 中文分词器: ES 已安装 elasticsearch-analysis-ik 插件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "articles")
@Setting(settingPath = "elasticsearch/ik-settings.json")
public class ArticleDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_smart", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    @Field(type = FieldType.Text, analyzer = "ik_smart", searchAnalyzer = "ik_smart")
    private String summary;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Date)
    private String createdAt;

    @Field(type = FieldType.Date)
    private String updatedAt;
}