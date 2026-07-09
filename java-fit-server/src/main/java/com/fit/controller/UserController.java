package com.fit.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fit.common.Result;
import com.fit.dto.ChangePasswordDTO;
import com.fit.dto.UpdateProfileDTO;
import com.fit.entity.User;
import com.fit.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

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
     * 完善用户资料（nickname + avatarUrl），status 0 → 1
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
        user.setStatus(1);
        userService.updateById(user);

        log.info("用户资料已完善: userId={}, nickname={}", userId, dto.nickname());
        return Result.success("资料完善成功");
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
}
