package com.enumerate.eblog.service;

import com.enumerate.eblog.entity.Diary;
import com.enumerate.eblog.mapper.DiaryMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DiaryService {

    private final DiaryMapper diaryMapper;

    public DiaryService(DiaryMapper diaryMapper) {
        this.diaryMapper = diaryMapper;
    }

    public List<Diary> findAll() {
        return diaryMapper.findAll();
    }

    public Diary findById(Long id) {
        return diaryMapper.findById(id);
    }

    public Diary findByDate(LocalDate date) {
        return diaryMapper.findByDate(date);
    }

    public Diary save(Diary diary) {
        diaryMapper.insert(diary);
        return diaryMapper.findById(diary.getId());
    }

    public Diary update(Long id, Diary partial) {
        Diary existing = diaryMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("日记不存在");
        }

        Diary toUpdate = new Diary();
        toUpdate.setId(id);
        if (partial.getDate() != null) toUpdate.setDate(partial.getDate());
        if (partial.getContent() != null) toUpdate.setContent(partial.getContent());
        if (partial.getMood() != null) toUpdate.setMood(partial.getMood());
        if (partial.getWeather() != null) toUpdate.setWeather(partial.getWeather());

        diaryMapper.update(toUpdate);
        return diaryMapper.findById(id);
    }

    public void delete(Long id) {
        if (diaryMapper.findById(id) == null) {
            throw new RuntimeException("日记不存在");
        }
        diaryMapper.deleteById(id);
    }
}