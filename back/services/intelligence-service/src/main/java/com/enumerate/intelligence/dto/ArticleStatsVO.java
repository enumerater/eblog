package com.enumerate.intelligence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleStatsVO {
    private int wordCount;
    private int readingTimeMinutes;
    private List<String> keywords;
    private int paragraphCount;
    private int imageCount;
}