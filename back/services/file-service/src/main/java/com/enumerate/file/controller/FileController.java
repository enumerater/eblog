package com.enumerate.file.controller;

import com.enumerate.common.core.constant.CommonConstants;
import com.enumerate.common.core.result.Result;
import com.enumerate.file.dto.FileVO;
import com.enumerate.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId,
            @RequestParam(required = false) String storageType) {
        return Result.success(fileService.uploadFile(file, userId, storageType));
    }

    @GetMapping("/{id}")
    public Result<FileVO> getFileInfo(@PathVariable Long id) {
        return Result.success(fileService.getFileInfo(id));
    }

    @GetMapping
    public Result<List<FileVO>> getFileList(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(fileService.getFileList(type, page, size));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        fileService.deleteFile(id);
        return Result.success();
    }

    @DeleteMapping("/cleanup")
    public Result<Long> cleanupUnreferenced() {
        return Result.success(fileService.cleanupUnreferenced());
    }
}