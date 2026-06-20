package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trip_tag_item")
public class TripTagItem {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 标签ID */
    private String tagId;

    /** 物品ID */
    private String itemId;

    /** 精简级别(MINIMAL/BASIC/ALL_INCLUSIVE) */
    private String versionLevel;

    /** 数量计算表达式(FIXED/DAY*1/DAY/2/LIMIT_MAX_4) */
    private String multiplierExpr;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
