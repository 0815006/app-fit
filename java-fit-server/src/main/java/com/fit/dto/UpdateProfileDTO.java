package com.fit.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户资料完善请求 DTO
 */
public record UpdateProfileDTO(
        @NotBlank(message = "昵称不能为空")
        String nickname,

        String avatarUrl
) {}
