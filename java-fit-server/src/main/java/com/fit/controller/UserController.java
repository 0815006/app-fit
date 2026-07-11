package com.fit.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fit.common.Result;
import com.fit.dto.ChangePasswordDTO;
import com.fit.dto.UpdateProfileDTO;
import com.fit.dto.UserWithStatsDTO;
import com.fit.entity.User;
import com.fit.service.LoginRecordService;
import com.fit.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户信息 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final UserService userService;
    private final LoginRecordService loginRecordService;

    /**
     * 获取当前登录用户完整信息
     */
    @GetMapping("/current")
    public Result<User> getCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        // 脱敏：不返回密码
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 完善/更新用户资料（nickname + avatarUrl + empNo）
     */
    @PostMapping("/update-profile")
    public Result<String> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        user.setNickname(dto.nickname());
        if (dto.avatarUrl() != null && !dto.avatarUrl().isBlank()) {
            user.setAvatarUrl(dto.avatarUrl());
        }
        // 支持维护工号：不传或传 "0000000" 表示未维护
        if (dto.empNo() != null && !"0000000".equals(dto.empNo())) {
            user.setEmpNo(dto.empNo());
        }
        // 支持维护员工姓名
        if (dto.empName() != null && !dto.empName().isBlank()) {
            user.setEmpName(dto.empName().trim());
        }
        user.setStatus(1);
        userService.updateById(user);

        log.info("用户资料已更新: userId={}, nickname={}, empNo={}, empName={}", userId, dto.nickname(), dto.empNo(), dto.empName());
        return Result.success("资料更新成功");
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public Result<String> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 校验旧密码
        if (user.getPassword() == null || !ENCODER.matches(dto.oldPassword(), user.getPassword())) {
            return Result.error(400, "旧密码不正确");
        }

        // 更新密码
        user.setPassword(ENCODER.encode(dto.newPassword()));
        userService.updateById(user);

        log.info("密码修改成功: userId={}", userId);
        return Result.success("密码修改成功");
    }

    /**
     * 获取所有用户列表（含登录次数 + 最近登录时间）
     */
    @GetMapping("/list-with-stats")
    public Result<List<UserWithStatsDTO>> listWithStats() {
        List<User> users = userService.list();
        List<UserWithStatsDTO> result = new ArrayList<>();

        for (User user : users) {
            String uid = user.getId();
            long loginCount = loginRecordService.countByUserId(uid);
            java.time.LocalDateTime lastLogin = loginRecordService.getLatestLoginTimeByUserId(uid);

            result.add(new UserWithStatsDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getWxOpenid(),
                    user.getEmpNo(),
                    user.getEmpName(),
                    user.getNickname(),
                    user.getAvatarUrl(),
                    user.getStatus(),
                    loginCount,
                    lastLogin,
                    user.getCreateTime(),
                    user.getUpdateTime()
            ));
        }

        return Result.success(result);
    }
}
