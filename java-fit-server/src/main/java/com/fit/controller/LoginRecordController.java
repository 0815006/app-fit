package com.fit.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fit.common.EmpContext;
import com.fit.common.Result;
import com.fit.dto.MiniProgramStatsDTO;
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
     * Record a login event for the current user (empNo from EmpContext,
     * which is auto-populated via Sa-Token or X-Emp-No header).
     * Accepts JSON body: { "loginType": "WEB" } or { "loginType": "MINI_PROGRAM" }
     */
    @PostMapping("/login-record")
    public Result<LoginRecord> recordLogin(@RequestBody Map<String, String> body) {
        String loginType = body.getOrDefault("loginType", "UNKNOWN");
        String empNo = EmpContext.getEmpNo();
        LoginRecord record = loginRecordService.record(empNo, loginType);
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
     * Get mini-program login stats:
     * - myCount: current Sa-Token user's total login count (by empNo)
     * - totalCount: all MINI_PROGRAM login records in history
     * (includes anonymous "0000000" records from before silent-login era)
     */
    @GetMapping("/login-record/mini-program-stats")
    public Result<MiniProgramStatsDTO> getMiniProgramStats() {
        // Read current user's empNo via Sa-Token
        String myEmpNo = "0000000";
        try {
            long userId = StpUtil.getLoginIdAsLong();
            User user = userService.getById(userId);
            if (user != null && user.getEmpNo() != null && !user.getEmpNo().isBlank()) {
                myEmpNo = user.getEmpNo();
            }
        } catch (Exception e) {
            log.debug("Sa-Token 未登录或用户不存在，empNo fallback to 0000000");
        }

        long myCount = loginRecordService.count(myEmpNo);
        long totalCount = loginRecordService.countByLoginType("MINI_PROGRAM");

        return Result.success(new MiniProgramStatsDTO(myCount, totalCount));
    }
}
