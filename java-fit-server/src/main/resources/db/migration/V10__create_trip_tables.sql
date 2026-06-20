-- =====================================================
-- V10: 出行清单模块建表脚本
-- =====================================================

-- 1. 物品主表 (trip_item)
CREATE TABLE `trip_item` (
    `id` VARCHAR(32) NOT NULL COMMENT 'String主键ID',
    `user_id` VARCHAR(32) NULL COMMENT '所属用户ID(NULL代表全局公共物品，隔离隐私)',
    `name` VARCHAR(100) NOT NULL COMMENT '物品名称',
    `category` VARCHAR(50) NOT NULL COMMENT '物品分类(数码、衣物等)',
    `default_container` VARCHAR(32) NOT NULL COMMENT '默认载体(SUITCASE/BACKPACK/POCKET)',
    `importance_level` VARCHAR(20) NOT NULL DEFAULT 'IMPORTANT' COMMENT '重要级别(CRITICAL/IMPORTANT/OPTIONAL)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品主库表';

-- 2. 上下文标签表 (trip_tag)
CREATE TABLE `trip_tag` (
    `id` VARCHAR(32) NOT NULL COMMENT 'String主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '标签名称(如:游泳/雨天/出差)',
    `type` VARCHAR(32) NOT NULL COMMENT '标签维度(SCENARIO/ACTIVITY/USER_GENDER/WEATHER/TRANSPORT/ACCOMMODATION)',
    `is_preset` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否官方预置(1-是,0-用户自建)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上下文标签表';

-- 3. 标签与物品关联规则表 (trip_tag_item)
CREATE TABLE `trip_tag_item` (
    `id` VARCHAR(32) NOT NULL COMMENT 'String主键ID',
    `tag_id` VARCHAR(32) NOT NULL COMMENT '标签ID',
    `item_id` VARCHAR(32) NOT NULL COMMENT '物品ID',
    `version_level` VARCHAR(32) NOT NULL DEFAULT 'BASIC' COMMENT '精简级别(MINIMAL/BASIC/ALL_INCLUSIVE)',
    `multiplier_expr` VARCHAR(128) NOT NULL DEFAULT 'FIXED' COMMENT '数量计算表达式(FIXED/DAY*1/DAY/2/LIMIT_MAX_4)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_item` (`tag_id`, `item_id`),
    KEY `idx_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签物品关联规则表';

-- 4. 出行计划实例表 (trip_plan)
CREATE TABLE `trip_plan` (
    `id` VARCHAR(32) NOT NULL COMMENT 'String主键ID',
    `user_id` VARCHAR(32) NOT NULL COMMENT '创建用户ID(工号)',
    `title` VARCHAR(150) NOT NULL COMMENT '计划标题(如:8月三亚度假)',
    `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '计划状态(DRAFT/PACKING/FINISHED/ARCHIVED)',
    `trip_days` INT NOT NULL DEFAULT 1 COMMENT '出行天数参数(日常高频场景默认为1)',
    `destination` VARCHAR(100) NULL COMMENT '目的地名称',
    `departure_time` DATETIME NULL COMMENT '出发时间',
    `return_time` DATETIME NULL COMMENT '返回时间',
    `is_public` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否公开分享为社区模板(预留)',
    `parent_template_id` VARCHAR(32) NULL COMMENT '继承的母模板ID(预留)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出行计划实例表';

-- 5. 计划物品核对明细表 (trip_plan_detail)
CREATE TABLE `trip_plan_detail` (
    `id` VARCHAR(32) NOT NULL COMMENT 'String主键ID',
    `plan_id` VARCHAR(32) NOT NULL COMMENT '计划实例ID',
    `item_id` VARCHAR(32) NOT NULL COMMENT '原物品ID',
    `item_name` VARCHAR(100) NOT NULL COMMENT '物品名称冗余',
    `container` VARCHAR(32) NOT NULL COMMENT '当前实际物理载体(支持降级动态修改)',
    `importance_level` VARCHAR(20) NOT NULL COMMENT '冗余重要级别(CRITICAL/IMPORTANT/OPTIONAL)',
    `target_quantity` INT NOT NULL DEFAULT 1 COMMENT '公式计算出的目标携带数量',
    `is_checked` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已装箱(0-否,1-是)',
    `exclude_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否本次临时排除(0-否,1-是)',
    `source_contexts_json` JSON NOT NULL COMMENT '多来源血缘快照展示JSON',
    `version_no` BIGINT NOT NULL DEFAULT 1 COMMENT '字段原子化同步版本号（V1单机版可设为常态默认值）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '服务端最后修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_plan` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划物品核对明细表';

-- 6. 计划物品多重血缘来源表 (trip_plan_source)
CREATE TABLE `trip_plan_source` (
    `id` VARCHAR(32) NOT NULL COMMENT 'String主键ID',
    `plan_detail_id` VARCHAR(32) NOT NULL COMMENT '明细表(trip_plan_detail)外键ID',
    `tag_id` VARCHAR(32) NOT NULL COMMENT '关联触发的上下文标签ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_plan_detail` (`plan_detail_id`),
    KEY `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划物品多重血缘来源表';
