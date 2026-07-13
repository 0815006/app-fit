package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_subscribe_quota")
public class UserSubscribeQuota {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 用户工号 */
    private String empNo;

    /** 微信订阅消息模板ID */
    private String templateId;

    /** 剩余可用推送次数 */
    private Integer remainingCount;

    /** 推送开关：1-开启，0-关闭 */
    private Integer pushEnabled;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
