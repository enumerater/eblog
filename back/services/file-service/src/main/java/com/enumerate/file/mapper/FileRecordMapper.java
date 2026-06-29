package com.enumerate.file.mapper;

import com.enumerate.file.entity.FileRecord;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface FileRecordMapper {
    FileRecord findById(@Param("id") Long id);
    FileRecord findByMd5(@Param("md5") String md5);
    List<FileRecord> findPage(@Param("offset") int offset, @Param("limit") int limit,
                              @Param("mimeType") String mimeType);
    long countAll(@Param("mimeType") String mimeType);
    void insert(FileRecord record);
    void deleteById(@Param("id") Long id);
}