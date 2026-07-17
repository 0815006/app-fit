package com.fit.controller;

import com.fit.common.EmpContext;
import com.fit.common.Result;
import com.fit.service.SubscribeQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/subscribe-quota")
@RequiredArgsConstructor
public class SubscribeQuotaController {

    private final SubscribeQuotaService service;

    /**
     * 增加订阅次数
     */
    @PostMapping("/increment")
    public Result<Map<String, Object>> increment(@RequestBody Map<String, Object> body) {
        String userId = EmpContext.getUserId();
        String templateId = (String) body.get("templateId");
        int count = body.containsKey("count") ? ((Number) body.get("count")).intValue() : 1;

        if (templateId == null || templateId.isBlank()) {
            return Result.error("templateId 不能为空");
        }

        Map<String, Object> data = service.increment(userId, templateId, count);
        return Result.success(data);
    }

    /**
     * 浏览菜品攒次数（受每日 5 次上限约束）
     */
    @PostMapping("/browse")
    public Result<Map<String, Object>> browse(@RequestBody Map<String, Object> body) {
        String userId = EmpContext.getUserId();
        String templateId = (String) body.get("templateId");

        if (templateId == null || templateId.isBlank()) {
            return Result.error("templateId 不能为空");
        }

        try {
            Map<String, Object> data = service.browseIncrement(userId, templateId);
            return Result.success(data);
        } catch (RuntimeException e) {
            if ("DAILY_LIMIT".equals(e.getMessage())) {
                return Result.error("今日浏览攒次数已达上限（5次），请明天再来");
            }
            throw e;
        }
    }

    /**
     * 查询当前用户订阅次数
     */
    @GetMapping
    public Result<Map<String, Object>> query(@RequestParam String templateId) {
        String userId = EmpContext.getUserId();
        Map<String, Object> data = service.query(userId, templateId);
        return Result.success(data);
    }

    /**
     * 切换推送开关
     */
    @PostMapping("/toggle-push")
    public Result<Map<String, Object>> togglePush(@RequestBody Map<String, Object> body) {
        String userId = EmpContext.getUserId();
        String templateId = (String) body.get("templateId");
        boolean pushEnabled = Boolean.TRUE.equals(body.get("pushEnabled"));

        if (templateId == null || templateId.isBlank()) {
            return Result.error("templateId 不能为空");
        }

        Map<String, Object> data = service.togglePush(userId, templateId, pushEnabled);
        return Result.success(data);
    }
}
