-- ============================================================
-- Flyway V23: 全量重建 gym_muscle 肌群字典
-- 基于 PRD v2.0 三、gym_muscle 肌群字典改造详细清单
-- 核心变更：
--   1. DELETE 旧数据 MUS0001~MUS0018，重新 INSERT 23 条新记录
--   2. 胸部拆分：胸大肌 → 胸上束/胸中束/胸下束
--   3. 背部新增：大圆肌/小圆肌；竖脊肌从 CORE 移至 BACK
--   4. 核心新增：DEEP_CORE（腹横肌等深层稳定肌群）
--   5. 新增 CARDIO 大类：CARDIO_GENERAL
--   6. 恢复时间修正：BACK 48→72h，SHOULDER 24→48h
-- ============================================================

-- 清空旧肌群数据
DELETE FROM gym_muscle;

-- 重新插入 23 条精细化肌肉记录
INSERT INTO gym_muscle (id, muscle_code, muscle_name, muscle_group, sort_no, base_recovery_hours) VALUES
-- 胸部 CHEST（3 块）：恢复 48h
('MUS0101', 'UPPER_CHEST',   '胸上束',  'CHEST',    1,  48),
('MUS0102', 'MID_CHEST',     '胸中束',  'CHEST',    2,  48),
('MUS0103', 'LOWER_CHEST',   '胸下束',  'CHEST',    3,  48),

-- 背部 BACK（5 块）：恢复 72h（修正）
('MUS0201', 'LATISSIMUS_DORSI', '背阔肌',        'BACK', 4,  72),
('MUS0202', 'TRAPEZIUS',        '斜方肌',        'BACK', 5,  72),
('MUS0203', 'RHOMBOID',         '菱形肌',        'BACK', 6,  72),
('MUS0204', 'TERES_MAJOR_MINOR','大圆肌/小圆肌',  'BACK', 7,  72),
('MUS0205', 'ERECTOR_SPINAE',   '竖脊肌',        'BACK', 8,  72),

-- 肩部 SHOULDER（3 块）：恢复 48h（修正）
('MUS0301', 'DELTOID_FRONT', '三角肌前束', 'SHOULDER', 9,  48),
('MUS0302', 'DELTOID_SIDE',  '三角肌中束', 'SHOULDER', 10, 48),
('MUS0303', 'DELTOID_REAR',  '三角肌后束', 'SHOULDER', 11, 48),

-- 手臂 ARM（3 块）：恢复 24h
('MUS0401', 'BICEPS',   '肱二头肌', 'ARM', 12, 24),
('MUS0402', 'TRICEPS',  '肱三头肌', 'ARM', 13, 24),
('MUS0403', 'FOREARM',  '前臂肌群', 'ARM', 14, 24),

-- 腿部 LEG（3 块）：恢复 72h
('MUS0501', 'QUADRICEPS', '股四头肌',   'LEG', 15, 72),
('MUS0502', 'HAMSTRING',  '腘绳肌',     'LEG', 16, 72),
('MUS0503', 'CALF',       '小腿三头肌',  'LEG', 17, 72),

-- 臀部 GLUTE（2 块）：恢复 48h
('MUS0601', 'GLUTEUS_MAXIMUS', '臀大肌', 'GLUTE', 18, 48),
('MUS0602', 'GLUTEUS_MEDIUS',  '臀中肌', 'GLUTE', 19, 48),

-- 核心 CORE（3 块）：恢复 24h
('MUS0701', 'RECTUS_ABDOMINIS', '腹直肌',         'CORE', 20, 24),
('MUS0702', 'OBLIQUE',          '腹斜肌',         'CORE', 21, 24),
('MUS0703', 'DEEP_CORE',        '核心深层肌群',    'CORE', 22, 24),

-- 有氧/拉伸 CARDIO（1 块，新增）：恢复 24h
('MUS0801', 'CARDIO_GENERAL', '有氧/拉伸', 'CARDIO', 23, 24);
