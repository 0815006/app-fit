package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("gym_action_equipment_rel")
public class GymActionEquipmentRel {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String actionId;
    private String equipmentId;

    /** display-only: action name */
    @TableField(exist = false)
    private String actionName;

    /** display-only: equipment name */
    @TableField(exist = false)
    private String equipmentName;
}
