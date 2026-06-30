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

import java.util.List;

/**
 * Elasticsearch 文章文档
 *
 * 索引配置:
 *   - title / summary:   Text, 标准分词
 *   - content:           Text, 全文检索
 *   - tags:              Keyword, 精确匹配 + 聚合
 *
 * IK 中文分词 (可选):
 *   安装 IK 插件后, 将 analyzer 改为 "ik_smart" / "ik_max_word"
 *   安装命令: docker exec eblog-es elasticsearch-plugin install -b
 *     https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.17.0/elasticsearch-analysis-ik-8.17.0.zip
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "articles")
public class ArticleDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String content;

    @Field(type = FieldType.Text)
    private String summary;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Date)
    private String createdAt;

    @Field(type = FieldType.Date)
    private String updatedAt;
}