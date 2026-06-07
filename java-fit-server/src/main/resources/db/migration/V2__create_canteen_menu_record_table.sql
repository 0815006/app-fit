-- Flyway migration V2: Create canteen_menu_record table
-- Stores parsed canteen menu data imported from Excel files.

CREATE TABLE canteen_menu_record (
    id              VARCHAR(32)    NOT NULL COMMENT '主键UUID',
    canteen_zone    VARCHAR(20)    NOT NULL COMMENT '食堂区域：一期、二期',
    menu_date       DATE           NOT NULL COMMENT '具体日期',
    week_day        VARCHAR(10)    NOT NULL COMMENT '星期几',
    meal_type       VARCHAR(20)    NOT NULL COMMENT '餐次类型：早餐、午餐、晚餐、夜宵',
    category_name   VARCHAR(100)   NOT NULL COMMENT '菜品细分类别',
    dish_name       VARCHAR(100)   NOT NULL COMMENT '菜品名称',
    unit            VARCHAR(20)    DEFAULT '份'  COMMENT '单位（份、碗、只等）',
    price           DECIMAL(10,2)  DEFAULT 0    COMMENT '价格',
    energy_kcal     INT            DEFAULT 0    COMMENT '能量(kcal)',
    is_spicy        TINYINT        DEFAULT 0    COMMENT '是否辣：0-否，1-是',
    import_batch_no VARCHAR(32)    NOT NULL COMMENT '导入批次号',
    create_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_menu_date (menu_date),
    INDEX idx_canteen_zone (canteen_zone),
    INDEX idx_meal_type (meal_type),
    INDEX idx_import_batch (import_batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食堂菜单记录表';
