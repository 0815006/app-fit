package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("push_message_history")
public class PushMessageHistory {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 用户工号 */
    private String empNo;

    /** 微信订阅消息模板ID */
    private String templateId;

    /** 推送的菜品名称（多个用逗号分隔） */
    private String dishNames;

    /** 食堂区域：一期、二期 */
    private String canteenZone;

    /** 餐次类型：早餐、午餐、晚餐、夜宵 */
    private String mealType;

    /** 菜单日期 */
    private LocalDate menuDate;

    /** 推送状态：SUCCESS、FAILED */
    private String pushStatus;

    /** 失败时的错误信息 */
    private String errorMessage;

    /** 推送时间 */
    private LocalDateTime sendTime;
}
