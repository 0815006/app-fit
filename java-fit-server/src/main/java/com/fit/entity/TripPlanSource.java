package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trip_plan_source")
public class TripPlanSource {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 明细表(trip_plan_detail)外键ID */
    private String planDetailId;

    /** 关联触发的上下文标签ID */
    private String tagId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
