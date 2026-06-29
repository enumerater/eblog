package com.enumerate.article.controller;

import com.aliyun.oss.OSS;
import com.enumerate.article.config.OssConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final OSS ossClient;
    private final OssConfig ossConfig;

    public UploadController(OSS ossClient, OssConfig ossConfig) {
        this.ossClient = ossClient;
        this.ossConfig = ossConfig;
    }

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "文件不能为空"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "只允许上传图片文件"));
        }

        // Generate unique file key: images/2025/06/20/{uuid}.{ext}
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        } else {
            // Fallback: derive ext from MIME type
            ext = switch (contentType) {
                case "image/png" -> ".png";
                case "image/gif" -> ".gif";
                case "image/webp" -> ".webp";
                case "image/svg+xml" -> ".svg";
                default -> ".jpg";
            };
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String key = "images/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

        try {
            ossClient.putObject(ossConfig.getBucketName(), key, file.getInputStream());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "文件上传失败: " + e.getMessage()));
        }

        // endpoint is like "https://oss-cn-hangzhou.aliyuncs.com" — extract host to build: https://{bucket}.{host}/{key}
        String endpointHost = ossConfig.getEndpoint().replace("https://", "").replace("http://", "");
        String url = "https://" + ossConfig.getBucketName() + "." + endpointHost + "/" + key;
        return ResponseEntity.ok(Map.of("url", url));
    }
}