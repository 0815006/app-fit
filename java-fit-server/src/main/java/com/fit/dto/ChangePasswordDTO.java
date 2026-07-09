package com.fit.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 修改密码请求 DTO
 */
public record ChangePasswordDTO(
        @NotBlank(message = "旧密码不能为空")
        String oldPassword,

        @NotBlank(message = "新密码不能为空")
        String newPassword
) {}
