CREATE TABLE user_favorite_dish (
    id              VARCHAR(32)  NOT NULL COMMENT '主键雪花ID',
    emp_no          VARCHAR(7)   NOT NULL COMMENT '用户工号',
    dish_name       VARCHAR(100) NOT NULL COMMENT '收藏的菜品名称',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_emp_dish (emp_no, dish_name),
    INDEX idx_emp_no (emp_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户菜品收藏表';
