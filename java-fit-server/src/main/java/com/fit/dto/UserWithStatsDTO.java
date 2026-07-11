package com.fit.dto;

import java.time.LocalDateTime;

/**
 * 用户列表 DTO —— 包含 User 基本信息 + 登录次数 + 最近登录时间
 */
public record UserWithStatsDTO(
        String id,
        String username,
        String wxOpenid,
        String empNo,
        String empName,
        String nickname,
        String avatarUrl,
        Integer status,
        long loginCount,
        LocalDateTime lastLoginTime,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {}
