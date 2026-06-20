package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trip_item")
public class TripItem {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 所属用户ID(NULL代表全局公共物品) */
    private String userId;

    /** 物品名称 */
    private String name;

    /** 物品分类(数码、衣物等) */
    private String category;

    /** 默认载体(SUITCASE/BACKPACK/POCKET) */
    private String defaultContainer;

    /** 重要级别(CRITICAL/IMPORTANT/OPTIONAL) */
    private String importanceLevel;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
