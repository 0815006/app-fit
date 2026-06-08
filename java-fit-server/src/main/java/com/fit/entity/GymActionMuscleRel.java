package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("gym_action_muscle_rel")
public class GymActionMuscleRel {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String actionId;
    private String muscleId;
    private Integer isPrimary;

    /** display-only: action name */
    @TableField(exist = false)
    private String actionName;

    /** display-only: muscle name */
    @TableField(exist = false)
    private String muscleName;
}
