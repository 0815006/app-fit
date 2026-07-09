package com.fit.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fit.common.Result;
import com.fit.dto.LoginResultDTO;
import com.fit.dto.WebLoginDTO;
import com.fit.dto.WxLoginDTO;
import com.fit.entity.User;
import com.fit.service.UserService;
import com.fit.service.WxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 Controller —— Web 端和小程序端登录
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final UserService userService;
    private final WxService wxService;

    /**
     * Web 端登录：用户名 + 密码
     */
    @PostMapping("/web-login")
    public Result<LoginResultDTO> webLogin(@RequestBody WebLoginDTO dto) {
        // 查用户
        User user = userService.getByUsername(dto.username());
        if (user == null || !ENCODER.matches(dto.password(), user.getPassword())) {
            return Result.error(401, "账号或密码错误");
        }

        // Sa-Token 登录
        StpUtil.login(user.getId());
        log.info("Web 登录成功: username={}, empNo={}", user.getUsername(), user.getEmpNo());

        // 脱敏：不返回密码
        user.setPassword(null);

        return Result.success(new LoginResultDTO(
                StpUtil.getTokenValue(),
                false,
                user
        ));
    }

    /**
     * 微信小程序登录：code → openid → 查/建用户
     */
    @PostMapping("/wx-login")
    public Result<LoginResultDTO> wxLogin(@RequestBody WxLoginDTO dto) {
        // 通过微信 code 换取 openid
        String openid = wxService.getOpenIdByCode(dto.code());

        // 按 openid 查找用户
        User user = userService.getByOpenid(openid);
        boolean isNewUser = false;

        if (user == null) {
            // 新用户：静默创建
            user = new User();
            user.setWxOpenid(openid);
            user.setStatus(0);
            userService.save(user);
            isNewUser = true;
            log.info("新用户静默注册: openid={}, userId={}", openid, user.getId());
        }

        // Sa-Token 登录
        StpUtil.login(user.getId());

        boolean needsProfile = user.getStatus() != null && user.getStatus() == 0;

        // 脱敏：不返回密码
        user.setPassword(null);

        return Result.success(new LoginResultDTO(
                StpUtil.getTokenValue(),
                needsProfile,
                user
        ));
    }
}
