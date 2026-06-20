package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trip_tag")
public class TripTag {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 标签名称(如:游泳/雨天/出差) */
    private String name;

    /** 标签维度(SCENARIO/ACTIVITY/USER_GENDER/WEATHER/TRANSPORT/ACCOMMODATION) */
    private String type;

    /** 是否官方预置(1-是,0-用户自建) */
    private Integer isPreset;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
