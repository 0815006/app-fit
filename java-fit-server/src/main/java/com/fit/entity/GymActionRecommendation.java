package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("gym_action_recommendation")
public class GymActionRecommendation {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String actionId;
    private String trainingGoal;
    private Integer minSets;
    private Integer maxSets;
    private Integer minReps;
    private Integer maxReps;
    private Integer recommendRestTime;
    private String intensityTips;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** display-only: action name */
    @TableField(exist = false)
    private String actionName;
}
