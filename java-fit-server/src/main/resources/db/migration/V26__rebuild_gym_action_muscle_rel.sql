-- ============================================================
-- Flyway V26: 全量重建 gym_action_muscle_rel 动作-肌群关联
-- 基于 PRD v2.0 六、gym_action_muscle_rel 动作-肌群关联规范
-- 每个动作 1 个主肌群（is_primary=1）+ N 个辅肌群（is_primary=0）
-- ============================================================

DELETE FROM gym_action_muscle_rel;

-- ============================================================
-- 胸部动作-肌群关联（17 动作 → 约 30 条）
-- ============================================================

-- ACT1001 上斜杠铃卧推 → UPPER_CHEST(主) + DELTOID_FRONT(辅) + TRICEPS(辅)
INSERT INTO gym_action_muscle_rel (id, action_id, muscle_id, is_primary) VALUES
('AMR1001', 'ACT1001', 'MUS0101', 1),
('AMR1002', 'ACT1001', 'MUS0301', 0),
('AMR1003', 'ACT1001', 'MUS0402', 0),

-- ACT1002 上斜哑铃卧推 → UPPER_CHEST(主) + DELTOID_FRONT(辅) + TRICEPS(辅)
('AMR1004', 'ACT1002', 'MUS0101', 1),
('AMR1005', 'ACT1002', 'MUS0301', 0),
('AMR1006', 'ACT1002', 'MUS0402', 0),

-- ACT1003 上斜哑铃飞鸟 → UPPER_CHEST(主)
('AMR1007', 'ACT1003', 'MUS0101', 1),

-- ACT1004 史密斯上斜卧推 → UPPER_CHEST(主) + DELTOID_FRONT(辅) + TRICEPS(辅)
('AMR1008', 'ACT1004', 'MUS0101', 1),
('AMR1009', 'ACT1004', 'MUS0301', 0),
('AMR1010', 'ACT1004', 'MUS0402', 0),

-- ACT1005 绳索低位上拉夹胸 → UPPER_CHEST(主)
('AMR1011', 'ACT1005', 'MUS0101', 1),

-- ACT1006 悍马机上斜推胸 → UPPER_CHEST(主) + DELTOID_FRONT(辅) + TRICEPS(辅)
('AMR1012', 'ACT1006', 'MUS0101', 1),
('AMR1013', 'ACT1006', 'MUS0301', 0),
('AMR1014', 'ACT1006', 'MUS0402', 0),

-- ACT1007 平板杠铃卧推 → MID_CHEST(主) + DELTOID_FRONT(辅) + TRICEPS(辅)
('AMR1015', 'ACT1007', 'MUS0102', 1),
('AMR1016', 'ACT1007', 'MUS0301', 0),
('AMR1017', 'ACT1007', 'MUS0402', 0),

-- ACT1008 平板哑铃卧推 → MID_CHEST(主) + DELTOID_FRONT(辅) + TRICEPS(辅)
('AMR1018', 'ACT1008', 'MUS0102', 1),
('AMR1019', 'ACT1008', 'MUS0301', 0),
('AMR1020', 'ACT1008', 'MUS0402', 0),

-- ACT1009 坐姿推胸机推胸 → MID_CHEST(主)
('AMR1021', 'ACT1009', 'MUS0102', 1),

-- ACT1010 蝴蝶机夹胸 → MID_CHEST(主)
('AMR1022', 'ACT1010', 'MUS0102', 1),

-- ACT1011 绳索中位水平夹胸 → MID_CHEST(主)
('AMR1023', 'ACT1011', 'MUS0102', 1),

-- ACT1012 平板哑铃飞鸟 → MID_CHEST(主)
('AMR1024', 'ACT1012', 'MUS0102', 1),

-- ACT1013 双杠臂屈伸（胸肌版） → LOWER_CHEST(主) + TRICEPS(辅)
('AMR1025', 'ACT1013', 'MUS0103', 1),
('AMR1026', 'ACT1013', 'MUS0402', 0),

-- ACT1014 下斜杠铃卧推 → LOWER_CHEST(主) + TRICEPS(辅) + DELTOID_FRONT(辅)
('AMR1027', 'ACT1014', 'MUS0103', 1),
('AMR1028', 'ACT1014', 'MUS0402', 0),
('AMR1029', 'ACT1014', 'MUS0301', 0),

-- ACT1015 下斜哑铃卧推 → LOWER_CHEST(主) + TRICEPS(辅)
('AMR1030', 'ACT1015', 'MUS0103', 1),
('AMR1031', 'ACT1015', 'MUS0402', 0),

-- ACT1016 绳索高位下压夹胸 → LOWER_CHEST(主)
('AMR1032', 'ACT1016', 'MUS0103', 1),

-- ACT1017 悍马机下斜推胸 → LOWER_CHEST(主) + TRICEPS(辅)
('AMR1033', 'ACT1017', 'MUS0103', 1),
('AMR1034', 'ACT1017', 'MUS0402', 0);

-- ============================================================
-- 背部动作-肌群关联（14 动作 → 约 32 条）
-- ============================================================
INSERT INTO gym_action_muscle_rel (id, action_id, muscle_id, is_primary) VALUES
-- ACT2001 正手高位下拉 → LATISSIMUS_DORSI(主) + BICEPS(辅) + RHOMBOID(辅)
('AMR2001', 'ACT2001', 'MUS0201', 1),
('AMR2002', 'ACT2001', 'MUS0401', 0),
('AMR2003', 'ACT2001', 'MUS0203', 0),

-- ACT2002 宽握引体向上 → LATISSIMUS_DORSI(主) + BICEPS(辅)
('AMR2004', 'ACT2002', 'MUS0201', 1),
('AMR2005', 'ACT2002', 'MUS0401', 0),

-- ACT2003 反握窄握高位下拉 → LATISSIMUS_DORSI(主) + BICEPS(辅)
('AMR2006', 'ACT2003', 'MUS0201', 1),
('AMR2007', 'ACT2003', 'MUS0401', 0),

-- ACT2004 直臂下压 → LATISSIMUS_DORSI(主)
('AMR2008', 'ACT2004', 'MUS0201', 1),

-- ACT2005 单臂哑铃划船 → LATISSIMUS_DORSI(主) + BICEPS(辅)
('AMR2009', 'ACT2005', 'MUS0201', 1),
('AMR2010', 'ACT2005', 'MUS0401', 0),

-- ACT2006 悍马机单臂下拉 → LATISSIMUS_DORSI(主)
('AMR2011', 'ACT2006', 'MUS0201', 1),

-- ACT2007 俯身杠铃划船 → TRAPEZIUS(主) + LATISSIMUS_DORSI(辅) + BICEPS(辅)
('AMR2012', 'ACT2007', 'MUS0202', 1),
('AMR2013', 'ACT2007', 'MUS0201', 0),
('AMR2014', 'ACT2007', 'MUS0401', 0),

-- ACT2008 坐姿绳索划船 → TRAPEZIUS(主) + RHOMBOID(辅) + BICEPS(辅)
('AMR2015', 'ACT2008', 'MUS0202', 1),
('AMR2016', 'ACT2008', 'MUS0203', 0),
('AMR2017', 'ACT2008', 'MUS0401', 0),

-- ACT2009 T杠划船 → TRAPEZIUS(主) + LATISSIMUS_DORSI(辅)
('AMR2018', 'ACT2009', 'MUS0202', 1),
('AMR2019', 'ACT2009', 'MUS0201', 0),

-- ACT2011 蝴蝶机反向飞鸟 → TRAPEZIUS(主) + DELTOID_REAR(辅)
('AMR2020', 'ACT2011', 'MUS0202', 1),
('AMR2021', 'ACT2011', 'MUS0303', 0),

-- ACT2012 哑铃耸肩 → TRAPEZIUS(主)
('AMR2022', 'ACT2012', 'MUS0202', 1),

-- ACT2013 传统硬拉 → ERECTOR_SPINAE(主) + HAMSTRING(辅) + GLUTEUS_MAXIMUS(辅) + TRAPEZIUS(辅)
('AMR2023', 'ACT2013', 'MUS0205', 1),
('AMR2024', 'ACT2013', 'MUS0502', 0),
('AMR2025', 'ACT2013', 'MUS0601', 0),
('AMR2026', 'ACT2013', 'MUS0202', 0),

-- ACT2014 罗马尼亚硬拉 → ERECTOR_SPINAE(主) + HAMSTRING(辅) + GLUTEUS_MAXIMUS(辅)
('AMR2027', 'ACT2014', 'MUS0205', 1),
('AMR2028', 'ACT2014', 'MUS0502', 0),
('AMR2029', 'ACT2014', 'MUS0601', 0),

-- ACT2015 山羊挺身 → ERECTOR_SPINAE(主)
('AMR2030', 'ACT2015', 'MUS0205', 1);

-- ============================================================
-- 腿部与臀部动作-肌群关联（15 动作 → 约 27 条）
-- ============================================================
INSERT INTO gym_action_muscle_rel (id, action_id, muscle_id, is_primary) VALUES
-- ACT3001 杠铃高位深蹲 → QUADRICEPS(主) + GLUTEUS_MAXIMUS(辅) + HAMSTRING(辅)
('AMR3001', 'ACT3001', 'MUS0501', 1),
('AMR3002', 'ACT3001', 'MUS0601', 0),
('AMR3003', 'ACT3001', 'MUS0502', 0),

-- ACT3002 倒蹬机推举 → QUADRICEPS(主) + GLUTEUS_MAXIMUS(辅)
('AMR3004', 'ACT3002', 'MUS0501', 1),
('AMR3005', 'ACT3002', 'MUS0601', 0),

-- ACT3003 坐姿腿屈伸 → QUADRICEPS(主)
('AMR3006', 'ACT3003', 'MUS0501', 1),

-- ACT3004 哈克深蹲 → QUADRICEPS(主) + GLUTEUS_MAXIMUS(辅)
('AMR3007', 'ACT3004', 'MUS0501', 1),
('AMR3008', 'ACT3004', 'MUS0601', 0),

-- ACT3005 哑铃向后箭步蹲 → QUADRICEPS(主) + GLUTEUS_MAXIMUS(辅)
('AMR3009', 'ACT3005', 'MUS0501', 1),
('AMR3010', 'ACT3005', 'MUS0601', 0),

-- ACT3006 俯卧腿弯举 → HAMSTRING(主)
('AMR3011', 'ACT3006', 'MUS0502', 1),

-- ACT3007 坐姿腿弯举 → HAMSTRING(主)
('AMR3012', 'ACT3007', 'MUS0502', 1),

-- ACT3008 直腿硬拉 → HAMSTRING(主) + ERECTOR_SPINAE(辅) + GLUTEUS_MAXIMUS(辅)
('AMR3013', 'ACT3008', 'MUS0502', 1),
('AMR3014', 'ACT3008', 'MUS0205', 0),
('AMR3015', 'ACT3008', 'MUS0601', 0),

-- ACT3009 杠铃臀推 → GLUTEUS_MAXIMUS(主) + HAMSTRING(辅)
('AMR3016', 'ACT3009', 'MUS0601', 1),
('AMR3017', 'ACT3009', 'MUS0502', 0),

-- ACT3010 史密斯臀推 → GLUTEUS_MAXIMUS(主) + HAMSTRING(辅)
('AMR3018', 'ACT3010', 'MUS0601', 1),
('AMR3019', 'ACT3010', 'MUS0502', 0),

-- ACT3011 绳索后踢腿 → GLUTEUS_MAXIMUS(主)
('AMR3020', 'ACT3011', 'MUS0601', 1),

-- ACT3012 保加利亚单腿蹲 → GLUTEUS_MAXIMUS(主) + QUADRICEPS(辅)
('AMR3021', 'ACT3012', 'MUS0601', 1),
('AMR3022', 'ACT3012', 'MUS0501', 0),

-- ACT3013 坐姿器械外展 → GLUTEUS_MEDIUS(主)
('AMR3023', 'ACT3013', 'MUS0602', 1),

-- ACT3014 站姿杠铃提踵 → CALF(主)
('AMR3024', 'ACT3014', 'MUS0503', 1),

-- ACT3015 坐姿器械提踵 → CALF(主)
('AMR3025', 'ACT3015', 'MUS0503', 1);

-- ============================================================
-- 肩部动作-肌群关联（13 动作 → 约 22 条）
-- ============================================================
INSERT INTO gym_action_muscle_rel (id, action_id, muscle_id, is_primary) VALUES
-- ACT4001 坐姿哑铃推举 → DELTOID_FRONT(主) + DELTOID_SIDE(辅) + TRICEPS(辅)
('AMR4001', 'ACT4001', 'MUS0301', 1),
('AMR4002', 'ACT4001', 'MUS0302', 0),
('AMR4003', 'ACT4001', 'MUS0402', 0),

-- ACT4002 杠铃推举 → DELTOID_FRONT(主) + DELTOID_SIDE(辅) + TRICEPS(辅)
('AMR4004', 'ACT4002', 'MUS0301', 1),
('AMR4005', 'ACT4002', 'MUS0302', 0),
('AMR4006', 'ACT4002', 'MUS0402', 0),

-- ACT4003 史密斯坐姿推举 → DELTOID_FRONT(主) + TRICEPS(辅)
('AMR4007', 'ACT4003', 'MUS0301', 1),
('AMR4008', 'ACT4003', 'MUS0402', 0),

-- ACT4004 哑铃前平举 → DELTOID_FRONT(主)
('AMR4009', 'ACT4004', 'MUS0301', 1),

-- ACT4005 绳索前平举 → DELTOID_FRONT(主)
('AMR4010', 'ACT4005', 'MUS0301', 1),

-- ACT4006 哑铃侧平举 → DELTOID_SIDE(主)
('AMR4011', 'ACT4006', 'MUS0302', 1),

-- ACT4007 绳索单臂侧平举 → DELTOID_SIDE(主)
('AMR4012', 'ACT4007', 'MUS0302', 1),

-- ACT4008 杠铃直立划船 → DELTOID_SIDE(主) + TRAPEZIUS(辅)
('AMR4013', 'ACT4008', 'MUS0302', 1),
('AMR4014', 'ACT4008', 'MUS0202', 0),

-- ACT4009 器械侧平举 → DELTOID_SIDE(主)
('AMR4015', 'ACT4009', 'MUS0302', 1),

-- ACT4010 哑铃俯身侧平举 → DELTOID_REAR(主)
('AMR4016', 'ACT4010', 'MUS0303', 1),

-- ACT4011 蝴蝶机反向飞鸟（肩） → DELTOID_REAR(主)
('AMR4017', 'ACT4011', 'MUS0303', 1),

-- ACT4012 绳索面拉 → DELTOID_REAR(主) + TRAPEZIUS(辅)
('AMR4018', 'ACT4012', 'MUS0303', 1),
('AMR4019', 'ACT4012', 'MUS0202', 0),

-- ACT4013 上斜俯卧哑铃反向飞鸟 → DELTOID_REAR(主)
('AMR4020', 'ACT4013', 'MUS0303', 1);

-- ============================================================
-- 手臂动作-肌群关联（13 动作 → 约 19 条）
-- ============================================================
INSERT INTO gym_action_muscle_rel (id, action_id, muscle_id, is_primary) VALUES
-- ACT5001 EZ杆曲柄杠铃弯举 → BICEPS(主) + FOREARM(辅)
('AMR5001', 'ACT5001', 'MUS0401', 1),
('AMR5002', 'ACT5001', 'MUS0403', 0),

-- ACT5002 哑铃交替弯举 → BICEPS(主) + FOREARM(辅)
('AMR5003', 'ACT5002', 'MUS0401', 1),
('AMR5004', 'ACT5002', 'MUS0403', 0),

-- ACT5003 哑铃锤式弯举 → BICEPS(主) + FOREARM(辅)
('AMR5005', 'ACT5003', 'MUS0401', 1),
('AMR5006', 'ACT5003', 'MUS0403', 0),

-- ACT5004 牧师凳弯举 → BICEPS(主)
('AMR5007', 'ACT5004', 'MUS0401', 1),

-- ACT5005 绳索低位弯举 → BICEPS(主)
('AMR5008', 'ACT5005', 'MUS0401', 1),

-- ACT5006 上斜哑铃弯举 → BICEPS(主)
('AMR5009', 'ACT5006', 'MUS0401', 1),

-- ACT5007 绳索下压 → TRICEPS(主)
('AMR5010', 'ACT5007', 'MUS0402', 1),

-- ACT5008 窄握杠铃卧推 → TRICEPS(主) + MID_CHEST(辅)
('AMR5011', 'ACT5008', 'MUS0402', 1),
('AMR5012', 'ACT5008', 'MUS0102', 0),

-- ACT5009 仰卧杠铃臂屈伸 → TRICEPS(主)
('AMR5013', 'ACT5009', 'MUS0402', 1),

-- ACT5010 哑铃颈后臂屈伸 → TRICEPS(主)
('AMR5014', 'ACT5010', 'MUS0402', 1),

-- ACT5011 双杠臂屈伸（三头版） → TRICEPS(主) + LOWER_CHEST(辅)
('AMR5015', 'ACT5011', 'MUS0402', 1),
('AMR5016', 'ACT5011', 'MUS0103', 0),

-- ACT5012 绳索颈后过顶臂屈伸 → TRICEPS(主)
('AMR5017', 'ACT5012', 'MUS0402', 1),

-- ACT5013 杠铃正反握腕弯举 → FOREARM(主)
('AMR5018', 'ACT5013', 'MUS0403', 1);

-- ============================================================
-- 核心动作-肌群关联（10 动作 → 约 12 条）
-- ============================================================
INSERT INTO gym_action_muscle_rel (id, action_id, muscle_id, is_primary) VALUES
-- ACT6001 基础卷腹 → RECTUS_ABDOMINIS(主)
('AMR6001', 'ACT6001', 'MUS0701', 1),

-- ACT6002 仰卧抬腿 → RECTUS_ABDOMINIS(主)
('AMR6002', 'ACT6002', 'MUS0701', 1),

-- ACT6003 悬垂举腿 → RECTUS_ABDOMINIS(主)
('AMR6003', 'ACT6003', 'MUS0701', 1),

-- ACT6004 绳索跪姿卷腹 → RECTUS_ABDOMINIS(主)
('AMR6004', 'ACT6004', 'MUS0701', 1),

-- ACT6005 健腹轮卷腹 → RECTUS_ABDOMINIS(主)
('AMR6005', 'ACT6005', 'MUS0701', 1),

-- ACT6006 俄罗斯转体 → OBLIQUE(主)
('AMR6006', 'ACT6006', 'MUS0702', 1),

-- ACT6007 负重侧屈 → OBLIQUE(主)
('AMR6007', 'ACT6007', 'MUS0702', 1),

-- ACT6008 绳索伐木式转体 → OBLIQUE(主)
('AMR6008', 'ACT6008', 'MUS0702', 1),

-- ACT6009 平板支撑 → DEEP_CORE(主) + RECTUS_ABDOMINIS(辅) + OBLIQUE(辅)
('AMR6009', 'ACT6009', 'MUS0703', 1),
('AMR6010', 'ACT6009', 'MUS0701', 0),
('AMR6011', 'ACT6009', 'MUS0702', 0),

-- ACT6010 健腹轮推行 → DEEP_CORE(主) + RECTUS_ABDOMINIS(辅)
('AMR6012', 'ACT6010', 'MUS0703', 1),
('AMR6013', 'ACT6010', 'MUS0701', 0);

-- ============================================================
-- 有氧/拉伸动作-肌群关联（7 动作 → 7 条）
-- ============================================================
INSERT INTO gym_action_muscle_rel (id, action_id, muscle_id, is_primary) VALUES
('AMR7001', 'ACT7001', 'MUS0801', 1),
('AMR7002', 'ACT7002', 'MUS0801', 1),
('AMR7003', 'ACT7003', 'MUS0801', 1),
('AMR7004', 'ACT7004', 'MUS0801', 1),
('AMR7005', 'ACT7005', 'MUS0801', 1),
('AMR7006', 'ACT7006', 'MUS0801', 1),
('AMR7007', 'ACT7007', 'MUS0801', 1);
