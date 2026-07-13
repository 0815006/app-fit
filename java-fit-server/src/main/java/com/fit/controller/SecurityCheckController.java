package com.fit.controller;

import com.fit.common.Result;
import com.fit.service.SecurityCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 微信内容安全检测 Controller —— 仅暴露文本检测给前端调用
 * 图片检测由 UploadController 内部调用
 */
@Slf4j
@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityCheckController {

    // 放行路径：/api/security/** 已在 WebConfig 中放行登录校验

    private final SecurityCheckService securityCheckService;

    /**
     * 文本内容安全检测
     */
    @PostMapping("/check-text")
    public Result<Map<String, Boolean>> checkText(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return Result.success(Map.of("pass", true));
        }

        boolean pass = securityCheckService.checkText(content);
        if (pass) {
            return Result.success(Map.of("pass", true));
        } else {
            return Result.error(400, "内容含违规信息，请修改后重试");
        }
    }
}
