CREATE TABLE user_subscribe_quota (
    id              VARCHAR(32)  NOT NULL COMMENT '主键雪花ID',
    emp_no          VARCHAR(7)   NOT NULL COMMENT '用户工号',
    template_id     VARCHAR(64)  NOT NULL COMMENT '微信订阅消息模板ID',
    remaining_count INT          NOT NULL DEFAULT 0 COMMENT '剩余可用推送次数',
    push_enabled    TINYINT      NOT NULL DEFAULT 1 COMMENT '推送开关：1-开启，0-关闭',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_emp_template (emp_no, template_id),
    INDEX idx_emp_no (emp_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订阅消息次数表';
