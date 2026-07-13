package com.fit.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fit.common.Result;
import com.fit.service.SecurityCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传 Controller —— 头像上传内置内容安全检测
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    @Value("${app.upload.avatar-path:./uploads/avatar}")
    private String avatarPath;

    @Value("${app.upload.avatar-url-prefix:/uploads/avatar}")
    private String avatarUrlPrefix;

    private final SecurityCheckService securityCheckService;

    /**
     * 上传头像（内置图片内容安全检测）
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
            // 先读取字节用于安全检测
            byte[] imageBytes = file.getBytes();

            // 内容安全检测
            boolean safe = securityCheckService.checkImage(imageBytes);
            if (!safe) {
                log.warn("头像安全检测不通过: fileName={}, size={}", file.getOriginalFilename(), imageBytes.length);
                return Result.error(400, "内容含违规信息，请修改后重试");
            }

            // 确保目录存在 —— 转换为绝对路径确保与 WebConfig 静态资源映射一致
            Path uploadDir = Path.of(avatarPath).toAbsolutePath().normalize();
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
            Files.write(targetPath, imageBytes);

            log.info("头像已保存至: {}", targetPath.toAbsolutePath());

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
