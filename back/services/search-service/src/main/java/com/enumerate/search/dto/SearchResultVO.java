package com.enumerate.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultVO {
    private Long id;
    private String title;
    private String summary;
    private String highlight;
    private List<String> tags;
    private LocalDateTime createdAt;
}