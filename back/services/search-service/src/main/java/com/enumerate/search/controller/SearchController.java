package com.enumerate.search.controller;

import com.enumerate.common.core.result.Result;
import com.enumerate.search.dto.HotSearchVO;
import com.enumerate.search.dto.SearchResultVO;
import com.enumerate.search.dto.SearchSuggestionVO;
import com.enumerate.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/articles")
    public Result<List<SearchResultVO>> searchArticles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(searchService.searchArticles(keyword, tag, page, size));
    }

    @GetMapping("/suggestions")
    public Result<List<SearchSuggestionVO>> getSuggestions(
            @RequestParam String keyword) {
        return Result.success(searchService.getSuggestions(keyword));
    }

    @GetMapping("/hot")
    public Result<List<HotSearchVO>> getHotSearches(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(searchService.getHotSearches(limit));
    }
}