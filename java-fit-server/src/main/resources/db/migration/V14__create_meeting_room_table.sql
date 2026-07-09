-- Flyway migration V14: Create meeting_room table
-- 会议室资源表

CREATE TABLE meeting_room (
    id              VARCHAR(32)    NOT NULL COMMENT '主键雪花ID',
    name            VARCHAR(50)    NOT NULL COMMENT '会议室名称（如"会议室 A (301)"）',
    location        VARCHAR(100)   NOT NULL COMMENT '具体楼层位置',
    capacity        INT            NOT NULL DEFAULT 0 COMMENT '座位数',
    facilities      VARCHAR(500)   DEFAULT '[]' COMMENT '配套硬件标签JSON数组',
    photo_url       VARCHAR(500)   DEFAULT '' COMMENT '实景照片URL',
    is_active       TINYINT(1)     NOT NULL DEFAULT 1 COMMENT '是否上架：1-上架，0-下架',
    sort_order      INT            NOT NULL DEFAULT 0 COMMENT '排序权重，越小越靠前',
    create_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_is_active (is_active),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议室资源表';
