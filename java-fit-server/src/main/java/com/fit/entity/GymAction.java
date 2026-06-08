package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("gym_action")
public class GymAction {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String name;
    private String alias;
    private String pinyinBref;
    private String actionType;
    private String movementPattern;
    private Integer difficultyLevel;
    private String imageUrls;
    private String videoUrl;
    private String actionGuide;
    private String safetyTips;
    private String searchKeywords;
    private Integer isCommon;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
