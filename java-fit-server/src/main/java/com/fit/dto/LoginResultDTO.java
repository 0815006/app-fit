package com.fit.dto;

import com.fit.entity.User;

/**
 * 登录成功响应 DTO
 */
public record LoginResultDTO(
        String token,
        boolean isNewUser,
        User userInfo
) {}
