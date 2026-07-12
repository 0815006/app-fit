package com.fit.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 榜单统一 VO — 用于坚持榜、容量榜、1RM巅峰榜、进步榜
 */
@Data
@Builder
public class RankingItemVO {

    /** 排名（1-based） */
    private int rank;

    /** 用户ID */
    private String userId;

    /** 员工姓名 */
    private String empName;

    /** 用户昵称 */
    private String nickname;

    /** 7位工号 */
    private String empNo;

    /** 头像地址 */
    private String avatarUrl;

    /** 核心数据值（天数 / 容量kg / 1RMkg / 增长率%） */
    private BigDecimal value;

    /** 辅助数据1（如：容量榜的打卡天数） */
    private BigDecimal auxiliaryValue;

    /** 辅助数据2（如：1RM榜的深蹲值） */
    private BigDecimal auxiliaryValue2;

    /** 趋势标记（↑/↓/→） */
    private String trend;
}
