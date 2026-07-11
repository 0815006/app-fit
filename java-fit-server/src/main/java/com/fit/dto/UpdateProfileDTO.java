package com.fit.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户资料完善请求 DTO
 */
public record UpdateProfileDTO(
        @NotBlank(message = "昵称不能为空")
        String nickname,

        String avatarUrl,

        /** 7位工号，会替换原有工号；不传或传 "0000000" 视为未维护 */
        String empNo,

        /** 员工姓名 */
        String empName
) {}
