package com.fit.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

/**
 * 头像静态资源 Controller —— 直接读取文件返回，跨平台兼容
 * 路径: GET /uploads/avatar/{filename}
 * 注意: 不拦截登录态，因为图片 URL 不允许带 sa-token header
 */
@Slf4j
@RestController
public class AvatarResourceController {

    @Value("${app.upload.avatar-path:./uploads/avatar}")
    private String avatarPath;

    @GetMapping("/uploads/avatar/{filename}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        Path filePath = Path.of(avatarPath).toAbsolutePath().normalize().resolve(filename);
        FileSystemResource resource = new FileSystemResource(filePath);

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        // 根据扩展名推断 Content-Type
        String contentType = "image/jpeg";
        String name = filename.toLowerCase();
        if (name.endsWith(".png")) {
            contentType = "image/png";
        } else if (name.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (name.endsWith(".webp")) {
            contentType = "image/webp";
        } else if (name.endsWith(".bmp")) {
            contentType = "image/bmp";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(resource);
    }
}
