package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trip_plan")
public class TripPlan {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 创建用户ID(工号) */
    private String userId;

    /** 计划标题(如:8月三亚度假) */
    private String title;

    /** 计划状态(DRAFT/PACKING/FINISHED/ARCHIVED) */
    private String status;

    /** 出行天数参数(日常高频场景默认为1) */
    private Integer tripDays;

    /** 目的地名称 */
    private String destination;

    /** 出发时间 */
    private LocalDateTime departureTime;

    /** 返回时间 */
    private LocalDateTime returnTime;

    /** 是否公开分享为社区模板(预留) */
    private Integer isPublic;

    /** 继承的母模板ID(预留) */
    private String parentTemplateId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
