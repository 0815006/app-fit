package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户表 —— 统一存储 Web 端管理员和小程序微信用户
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** Web端登录用户名 */
    private String username;

    /** BCrypt加密密码 */
    private String password;

    /** 微信OpenID（小程序端） */
    private String wxOpenid;

    /** 7位工号 */
    private String empNo;

    /** 员工姓名 */
    private String empName;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatarUrl;

    /** 状态: 0=未完善资料, 1=已完善 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
