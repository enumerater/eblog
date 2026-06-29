package com.enumerate.file.dto;

import com.enumerate.file.entity.FileRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileVO {
    private Long id;
    private String originalName;
    private String url;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime createdAt;

    public static FileVO from(FileRecord record) {
        return FileVO.builder()
                .id(record.getId())
                .originalName(record.getOriginalName())
                .url(record.getStoredPath())
                .fileSize(record.getFileSize())
                .mimeType(record.getMimeType())
                .createdAt(record.getCreatedAt())
                .build();
    }
}