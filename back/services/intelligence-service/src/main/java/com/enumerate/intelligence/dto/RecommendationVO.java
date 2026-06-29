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
public class RecommendationVO {
    private Long id;
    private String title;
    private String summary;
    private List<String> tags;
    private double score;
}