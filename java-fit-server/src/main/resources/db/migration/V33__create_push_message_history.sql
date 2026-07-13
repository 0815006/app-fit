CREATE TABLE push_message_history (
    id              VARCHAR(32)  NOT NULL COMMENT '主键雪花ID',
    emp_no          VARCHAR(7)   NOT NULL COMMENT '用户工号',
    template_id     VARCHAR(64)  NOT NULL COMMENT '微信订阅消息模板ID',
    dish_names      VARCHAR(500) NOT NULL COMMENT '推送的菜品名称（多个用逗号分隔）',
    canteen_zone    VARCHAR(20)  NOT NULL COMMENT '食堂区域：一期、二期',
    meal_type       VARCHAR(20)  NOT NULL COMMENT '餐次类型：早餐、午餐、晚餐、夜宵',
    menu_date       DATE         NOT NULL COMMENT '菜单日期',
    push_status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '推送状态：SUCCESS、FAILED',
    error_message   VARCHAR(500) DEFAULT NULL COMMENT '失败时的错误信息',
    send_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推送时间',
    PRIMARY KEY (id),
    INDEX idx_emp_no (emp_no),
    INDEX idx_push_status (push_status),
    INDEX idx_send_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推送消息历史表';
