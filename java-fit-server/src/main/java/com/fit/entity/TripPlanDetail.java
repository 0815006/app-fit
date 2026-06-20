package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trip_plan_detail")
public class TripPlanDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 计划实例ID */
    private String planId;

    /** 原物品ID */
    private String itemId;

    /** 物品名称冗余 */
    private String itemName;

    /** 当前实际物理载体(支持降级动态修改) */
    private String container;

    /** 冗余重要级别(CRITICAL/IMPORTANT/OPTIONAL) */
    private String importanceLevel;

    /** 公式计算出的目标携带数量 */
    private Integer targetQuantity;

    /** 是否已装箱(0-否,1-是) */
    private Integer isChecked;

    /** 是否本次临时排除(0-否,1-是) */
    private Integer excludeFlag;

    /** 多来源血缘快照展示JSON */
    private String sourceContextsJson;

    /** 字段原子化同步版本号 */
    private Long versionNo;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
