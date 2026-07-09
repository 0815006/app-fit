package com.fit.dto;

/**
 * Web 端登录请求 DTO
 */
public record WebLoginDTO(
        String username,
        String password
) {}
