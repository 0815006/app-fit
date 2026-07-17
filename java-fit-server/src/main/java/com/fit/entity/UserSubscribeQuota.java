package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_subscribe_quota")
public class UserSubscribeQuota {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 用户ID（user表主键） */
    private String userId;

    /** 微信订阅消息模板ID */
    private String templateId;

    /** 剩余可用推送次数 */
    private Integer remainingCount;

    /** 推送开关：1-开启，0-关闭 */
    private Integer pushEnabled;

    /** 最近一次浏览攒次数日期 */
    private LocalDate lastBrowseDate;

    /** 今日已浏览攒次数 */
    private Integer todayBrowseCount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
