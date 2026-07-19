package com.enumerate.eblog.mapper;

import com.enumerate.eblog.entity.Draft;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DraftMapper {

    List<Draft> findAll();

    Draft findById(@Param("id") Long id);

    int insert(Draft draft);

    int update(Draft draft);

    int deleteById(@Param("id") Long id);
}