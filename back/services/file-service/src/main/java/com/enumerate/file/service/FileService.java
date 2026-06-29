package com.enumerate.file.service;

import com.enumerate.common.core.exception.BizException;
import com.enumerate.common.core.result.ResultCode;
import com.enumerate.file.dto.FileVO;
import com.enumerate.file.entity.FileRecord;
import com.enumerate.file.mapper.FileRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRecordMapper fileRecordMapper;
    private final Map<String, FileStorageService> storageServices;

    @Transactional
    public FileVO uploadFile(MultipartFile file, Long uploaderId, String storageType) {
        if (file.isEmpty()) {
            throw new BizException(400, "文件不能为空");
        }

        // 计算 MD5 用于去重
        String md5;
        try {
            md5 = DigestUtils.md5Hex(file.getInputStream());
            // 检查是否已存在相同 MD5 的文件
            FileRecord existing = fileRecordMapper.findByMd5(md5);
            if (existing != null) {
                log.info("检测到重复文件 (MD5: {}), 返回已有记录", md5);
                return FileVO.from(existing);
            }
        } catch (IOException e) {
            throw new BizException(500, "文件读取失败");
        }

        // 选择合适的存储后端
        String type = storageType != null ? storageType : "local";
        FileStorageService storageService = storageServices.get(type + "FileStorageService");
        if (storageService == null) {
            storageService = storageServices.values().iterator().next();
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "unknown";
        }

        // 存储文件
        String storedPath = storageService.store(file, originalName);

        // 记录到数据库
        FileRecord record = new FileRecord();
        record.setOriginalName(originalName);
        record.setStoredPath(storedPath);
        record.setFileSize(file.getSize());
        record.setMimeType(file.getContentType());
        record.setMd5(md5);
        record.setStorageType(storageService.getType());
        record.setUploaderId(uploaderId);
        fileRecordMapper.insert(record);

        return FileVO.from(record);
    }

    public FileVO getFileInfo(Long id) {
        FileRecord record = fileRecordMapper.findById(id);
        if (record == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "文件不存在");
        }
        return FileVO.from(record);
    }

    public List<FileVO> getFileList(String mimeType, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 10;
        int offset = (page - 1) * size;

        return fileRecordMapper.findPage(offset, size, mimeType).stream()
                .map(FileVO::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteFile(Long id) {
        FileRecord record = fileRecordMapper.findById(id);
        if (record == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "文件不存在");
        }

        // 删除物理文件
        String storageType = record.getStorageType().toLowerCase();
        FileStorageService storageService = storageServices.get(storageType + "FileStorageService");
        if (storageService != null) {
            storageService.delete(record.getStoredPath());
        }

        fileRecordMapper.deleteById(id);
        log.info("文件已删除: id={}, name={}", id, record.getOriginalName());
    }

    public long cleanupUnreferenced() {
        // 简单清理逻辑: 仅记录日志，实际可扩展为检查 article 内容中是否引用该文件
        log.info("清理未引用文件: 功能待扩展");
        return 0;
    }
}