package com.fit.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fit.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${app.upload.avatar-path:./uploads/avatar}")
    private String avatarPath;

    @Value("${app.upload.avatar-url-prefix:/uploads/avatar}")
    private String avatarUrlPrefix;

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        // 验证登录
        StpUtil.checkLogin();

        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只允许上传图片文件");
        }

        try {
            // 确保目录存在
            Path uploadDir = Paths.get(avatarPath);
            Files.createDirectories(uploadDir);

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path targetPath = uploadDir.resolve(filename);
            file.transferTo(targetPath.toFile());

            // 构造可访问 URL
            String url = avatarUrlPrefix + "/" + filename;

            log.info("头像上传成功: {}", url);
            return Result.success(Map.of("url", url));

        } catch (IOException e) {
            log.error("头像上传失败", e);
            return Result.error("头像上传失败: " + e.getMessage());
        }
    }
}
