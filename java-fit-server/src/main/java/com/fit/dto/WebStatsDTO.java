package com.fit.dto;

/**
 * Web端登录统计数据 DTO
 */
public record WebStatsDTO(
        /** 当前用户 WEB 端登录总次数（按 userId + WEB 汇总） */
        long myWebCount,
        /** WEB 端所有用户总登录次数（按 WEB 汇总） */
        long totalWebCount,
        /** 小程序端所有用户总登录次数（按 MINI_PROGRAM 汇总） */
        long totalMiniProgramCount,
        /** 全部总登录次数（WEB + MINI_PROGRAM） */
        long totalAllCount
) {}
