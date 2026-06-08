-- Flyway migration V5: Create gym_action table
-- Fitness action standard library.

CREATE TABLE gym_action (
    id                VARCHAR(32)   NOT NULL COMMENT '主键ID',
    name              VARCHAR(100)  NOT NULL COMMENT '动作名称（如：杠铃平板卧推）',
    alias             VARCHAR(200)  DEFAULT NULL COMMENT '别名/英文名（如：Bench Press）',
    pinyin_bref       VARCHAR(50)   DEFAULT NULL COMMENT '拼音简拼（如：glwt）',
    action_type       VARCHAR(30)   NOT NULL COMMENT '动作类型（COMPOUND:复合, ISOLATION:孤立, CARDIO:有氧, STRETCH:拉伸, MOBILITY:灵活性, PLYOMETRIC:爆发力）',
    movement_pattern  VARCHAR(30)   NOT NULL COMMENT '动作模式（PUSH:推, PULL:拉, SQUAT:深蹲, HINGE:髋铰链, LUNGE:弓步, CARRY:搬运, ROTATION:旋转, CORE:核心稳定, CARDIO:有氧）',
    difficulty_level  TINYINT       DEFAULT 1 COMMENT '难度等级（1:初学者, 2:中级, 3:进阶高级）',
    image_urls        JSON          DEFAULT NULL COMMENT '动作图片地址列表（JSON数组，如：["url1","url2"]）',
    video_url         VARCHAR(500)  DEFAULT NULL COMMENT '视频/GIF动态演示地址',
    action_guide      TEXT          COMMENT '动作要领详细文本',
    safety_tips       VARCHAR(1000) DEFAULT NULL COMMENT '安全提示/常见错误',
    search_keywords   JSON          DEFAULT NULL COMMENT '搜索关键字/标签（JSON数组）',
    is_common         TINYINT(1)    DEFAULT 1 COMMENT '是否常用（1:是, 0:否）',
    status            TINYINT(1)    DEFAULT 1 COMMENT '状态（1:启用, 0:禁用）',
    create_time       DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_name (name),
    KEY idx_pinyin (pinyin_bref),
    KEY idx_action_type (action_type),
    KEY idx_movement_pattern (movement_pattern)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健身动作标准库';
