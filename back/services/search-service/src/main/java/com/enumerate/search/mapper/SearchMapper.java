package com.enumerate.search.mapper;

import com.enumerate.search.entity.SearchLog;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface SearchMapper {
    void insertLog(SearchLog log);
    List<String> findTitleSuggestions(@Param("prefix") String prefix);
    List<String> findAllTitles();
    List<String> findDistinctKeywords(@Param("limit") int limit);
}