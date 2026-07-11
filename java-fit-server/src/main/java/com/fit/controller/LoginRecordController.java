package com.fit.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fit.common.EmpContext;
import com.fit.common.Result;
import com.fit.dto.MiniProgramStatsDTO;
import com.fit.dto.WebStatsDTO;
import com.fit.entity.LoginRecord;
import com.fit.entity.User;
import com.fit.service.LoginRecordService;
import com.fit.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LoginRecordController {

    private final LoginRecordService loginRecordService;
    private final UserService userService;

    /**
     * Record a login event for the current user.
     * Accepts JSON body: { "loginType": "WEB" } or { "loginType": "MINI_PROGRAM" }
     * userId is resolved from current Sa-Token session.
     */
    @PostMapping("/login-record")
    public Result<LoginRecord> recordLogin(@RequestBody Map<String, String> body) {
        String loginType = body.getOrDefault("loginType", "UNKNOWN");
        String empNo = EmpContext.getEmpNo();
        String userId = resolveUserId();
        LoginRecord record = loginRecordService.record(userId, empNo, loginType);
        return Result.success(record);
    }

    /**
     * Get total login count for a given employee.
     */
    @GetMapping("/login-record/count/{empNo}")
    public Result<Long> getCount(@PathVariable String empNo) {
        long count = loginRecordService.count(empNo);
        return Result.success(count);
    }

    /**
     * 小程序端登录统计：
     * - myCount: 当前用户在小程序端的总登录次数（按 userId + MINI_PROGRAM 汇总）
     * - totalCount: 小程序端所有用户总登录次数（按 MINI_PROGRAM 汇总）
     */
    @GetMapping("/login-record/mini-program-stats")
    public Result<MiniProgramStatsDTO> getMiniProgramStats() {
        String userId = resolveUserId();

        long myCount = 0;
        if (!"0000000".equals(userId)) {
            myCount = loginRecordService.countByUserIdAndLoginType(userId, "MINI_PROGRAM");
        }
        long totalCount = loginRecordService.countByLoginType("MINI_PROGRAM");

        return Result.success(new MiniProgramStatsDTO(myCount, totalCount));
    }

    /**
     * Web端登录统计：
     * - myWebCount: 当前用户在WEB端的总登录次数（按 userId + WEB 汇总）
     * - totalWebCount: WEB端所有用户总登录次数（按 WEB 汇总）
     * - totalMiniProgramCount: 小程序端所有用户总登录次数（按 MINI_PROGRAM 汇总）
     * - totalAllCount: 全部总登录次数（WEB + MINI_PROGRAM）
     */
    @GetMapping("/login-record/web-stats")
    public Result<WebStatsDTO> getWebStats() {
        String userId = resolveUserId();

        long myWebCount = 0;
        if (!"0000000".equals(userId)) {
            myWebCount = loginRecordService.countByUserIdAndLoginType(userId, "WEB");
        }
        long totalWebCount = loginRecordService.countByLoginType("WEB");
        long totalMiniProgramCount = loginRecordService.countByLoginType("MINI_PROGRAM");
        long totalAllCount = loginRecordService.countAll();

        return Result.success(new WebStatsDTO(myWebCount, totalWebCount, totalMiniProgramCount, totalAllCount));
    }

    /**
     * 从 Sa-Token 当前会话解析 user 表 ID，未登录返回 "0000000"
     */
    private String resolveUserId() {
        try {
            String userId = StpUtil.getLoginIdAsString();
            if (userId != null && !userId.isBlank()) {
                return userId;
            }
        } catch (Exception e) {
            log.debug("Sa-Token 未登录，userId fallback to 0000000");
        }
        return "0000000";
    }
}
