package com.enumerate.eblog.mapper;

import com.enumerate.eblog.entity.Diary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DiaryMapper {

    List<Diary> findAll();

    Diary findById(@Param("id") Long id);

    Diary findByDate(@Param("date") LocalDate date);

    int insert(Diary diary);

    int update(Diary diary);

    int deleteById(@Param("id") Long id);
}