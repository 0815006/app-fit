-- ============================================================
-- Flyway V24: 扩展 gym_equipment 器械字典
-- 基于 PRD v2.0 四、gym_equipment 器械字典改造详细清单
-- 保留 EQU0001~EQU0016，追加 EQU0017~EQU0034 共 18 种新器械
-- ============================================================

INSERT INTO gym_equipment (id, equipment_code, equipment_name, equipment_type) VALUES
('EQU0017', 'EZ_BAR',                 'EZ曲柄杠铃',           'FREE_WEIGHT'),
('EQU0018', 'PREACHER_BENCH',         '牧师凳',               'MACHINE'),
('EQU0019', 'FLAT_BENCH',             '平凳',                 'FREE_WEIGHT'),
('EQU0020', 'INCLINE_BENCH',          '上斜凳',               'FREE_WEIGHT'),
('EQU0021', 'DECLINE_BENCH',          '下斜凳',               'FREE_WEIGHT'),
('EQU0022', 'HYPEREXTENSION_BENCH',   '45度挺身架（罗马椅）',  'MACHINE'),
('EQU0023', 'SQUAT_RACK',             '深蹲架',               'MACHINE'),
('EQU0024', 'LEG_CURL_MACHINE',       '腿弯举机',             'MACHINE'),
('EQU0025', 'HACK_SQUAT_MACHINE',     '哈克深蹲机',           'MACHINE'),
('EQU0026', 'HIP_ADDUCTOR_MACHINE',   '夹腿机（外展/内收）',   'MACHINE'),
('EQU0027', 'CALF_RAISE_MACHINE',     '提踵机',               'MACHINE'),
('EQU0028', 'YOGA_MAT',               '瑜伽垫',               'FUNCTIONAL'),
('EQU0029', 'AB_WHEEL',               '健腹轮',               'FUNCTIONAL'),
('EQU0030', 'MEDICINE_BALL',          '药球',                 'FUNCTIONAL'),
('EQU0031', 'HAMMER_STRENGTH',        '悍马机（通用固定器械）', 'MACHINE'),
('EQU0032', 'LATERAL_RAISE_MACHINE',  '侧平举专用机',          'MACHINE'),
('EQU0033', 'GLUTE_DRIVE_MACHINE',    '臀推机',               'MACHINE'),
('EQU0034', 'ANKLE_STRAP',            '脚踝绑带',             'CABLE');
