package com.enumerate.eblog.controller;

import com.enumerate.eblog.entity.Diary;
import com.enumerate.eblog.service.DiaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diaries")
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        List<Diary> diaries = diaryService.findAll();
        return ResponseEntity.ok(diaries);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Diary diary = diaryService.findById(id);
        if (diary == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "日记不存在"));
        }
        return ResponseEntity.ok(diary);
    }

    @GetMapping("/date")
    public ResponseEntity<?> getByDate(@RequestParam("date") String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            Diary diary = diaryService.findByDate(date);
            if (diary == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "该日期没有日记"));
            }
            return ResponseEntity.ok(diary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "日期格式错误，请使用 yyyy-MM-dd"));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String dateStr = (String) body.get("date");
        String content = (String) body.get("content");

        if (dateStr == null || dateStr.isBlank() || content == null || content.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "日期和内容不能为空"));
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "日期格式错误，请使用 yyyy-MM-dd"));
        }

        // 检查该日期是否已有日记
        Diary existing = diaryService.findByDate(date);
        if (existing != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "该日期已有日记，请使用编辑功能"));
        }

        Diary diary = new Diary();
        diary.setDate(date);
        diary.setContent(content);

        if (body.containsKey("mood")) {
            diary.setMood((String) body.get("mood"));
        }
        if (body.containsKey("weather")) {
            diary.setWeather((String) body.get("weather"));
        }

        Diary saved = diaryService.save(diary);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Diary partial = new Diary();
            if (body.containsKey("date")) {
                partial.setDate(LocalDate.parse((String) body.get("date")));
            }
            if (body.containsKey("content")) {
                partial.setContent((String) body.get("content"));
            }
            if (body.containsKey("mood")) {
                partial.setMood((String) body.get("mood"));
            }
            if (body.containsKey("weather")) {
                partial.setWeather((String) body.get("weather"));
            }

            Diary updated = diaryService.update(id, partial);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "日记不存在"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            diaryService.delete(id);
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "日记不存在"));
        }
    }
}