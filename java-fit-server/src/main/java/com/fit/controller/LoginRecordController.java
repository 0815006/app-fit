package com.fit.controller;

import com.fit.common.EmpContext;
import com.fit.common.Result;
import com.fit.entity.LoginRecord;
import com.fit.service.LoginRecordService;
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

    /**
     * Record a login event for the current user (empNo from X-Emp-No header).
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
}
