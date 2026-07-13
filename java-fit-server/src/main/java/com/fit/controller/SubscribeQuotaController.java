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
        String empNo = EmpContext.getEmpNo();
        String templateId = (String) body.get("templateId");
        int count = body.containsKey("count") ? ((Number) body.get("count")).intValue() : 1;

        if (templateId == null || templateId.isBlank()) {
            return Result.error("templateId 不能为空");
        }

        Map<String, Object> data = service.increment(empNo, templateId, count);
        return Result.success(data);
    }

    /**
     * 查询当前用户订阅次数
     */
    @GetMapping
    public Result<Map<String, Object>> query(@RequestParam String templateId) {
        String empNo = EmpContext.getEmpNo();
        Map<String, Object> data = service.query(empNo, templateId);
        return Result.success(data);
    }

    /**
     * 切换推送开关
     */
    @PostMapping("/toggle-push")
    public Result<Map<String, Object>> togglePush(@RequestBody Map<String, Object> body) {
        String empNo = EmpContext.getEmpNo();
        String templateId = (String) body.get("templateId");
        boolean pushEnabled = Boolean.TRUE.equals(body.get("pushEnabled"));

        if (templateId == null || templateId.isBlank()) {
            return Result.error("templateId 不能为空");
        }

        Map<String, Object> data = service.togglePush(empNo, templateId, pushEnabled);
        return Result.success(data);
    }
}
