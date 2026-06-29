package com.enumerate.file.service.impl;

import com.enumerate.file.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "file.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.storage.local.path:./upload}")
    private String basePath;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(basePath));
            log.info("本地文件存储初始化完成: {}", basePath);
        } catch (IOException e) {
            log.error("创建上传目录失败: {}", e.getMessage());
        }
    }

    @Override
    public String store(MultipartFile file, String filename) {
        try {
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf(".")) : "";
            String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path targetDir = Paths.get(basePath, dateDir);
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(storedName);
            file.transferTo(targetFile.toFile());
            String relativePath = dateDir + "/" + storedName;
            log.info("文件已保存: {}", relativePath);
            return relativePath;
        } catch (IOException e) {
            throw new RuntimeException("文件存储失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Path file = Paths.get(basePath, path);
            Files.deleteIfExists(file);
            log.info("文件已删除: {}", path);
        } catch (IOException e) {
            log.warn("文件删除失败: {}", e.getMessage());
        }
    }

    @Override
    public String getType() {
        return "LOCAL";
    }
}