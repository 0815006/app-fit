-- ============================================================
-- Flyway V13: 系统用户表
-- 统一存储 Web 端管理员和小程序端微信用户
-- ============================================================
CREATE TABLE `user` (
    `id`          VARCHAR(32)  NOT NULL COMMENT 'Snowflake主键',
    `username`    VARCHAR(50)  NULL     COMMENT 'Web端登录用户名',
    `password`    VARCHAR(255) NULL     COMMENT 'BCrypt加密密码',
    `wx_openid`   VARCHAR(64)  NULL     COMMENT '微信OpenID(小程序端)',
    `emp_no`      VARCHAR(7)   NOT NULL DEFAULT '0000000' COMMENT '7位工号',
    `nickname`    VARCHAR(50)  NULL     COMMENT '昵称',
    `avatar_url`  VARCHAR(500) NULL     COMMENT '头像URL',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0=未完善资料, 1=已完善',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_wx_openid` (`wx_openid`),
    KEY `idx_emp_no` (`emp_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
