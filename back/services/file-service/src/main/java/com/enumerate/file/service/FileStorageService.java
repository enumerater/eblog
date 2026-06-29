package com.enumerate.file.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file, String filename);
    void delete(String path);
    String getType();
}