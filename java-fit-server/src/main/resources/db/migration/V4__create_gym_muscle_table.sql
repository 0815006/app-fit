-- Flyway migration V4: Create gym_muscle table + init data
-- Muscle group dictionary for fitness action library.

CREATE TABLE gym_muscle (
    id           VARCHAR(32)  NOT NULL COMMENT '主键ID',
    muscle_code  VARCHAR(50)  NOT NULL COMMENT '肌群编码（如：LATISSIMUS_DORSI）',
    muscle_name  VARCHAR(50)  NOT NULL COMMENT '肌群中文名称（如：背阔肌）',
    muscle_group VARCHAR(50)  NOT NULL COMMENT '所属肌群大类（CHEST:胸部, BACK:背部, SHOULDER:肩部, ARM:手臂, LEG:腿部, GLUTE:臀部, CORE:核心, FULL_BODY:全身）',
    sort_no      INT          DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (muscle_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='肌群字典';

-- Init data
INSERT INTO gym_muscle (id, muscle_code, muscle_name, muscle_group, sort_no) VALUES
('MUS0001', 'CHEST_MAJOR',        '胸大肌',     'CHEST',    1),
('MUS0002', 'LATISSIMUS_DORSI',   '背阔肌',     'BACK',     2),
('MUS0003', 'TRAPEZIUS',          '斜方肌',     'BACK',     3),
('MUS0004', 'RHOMBOID',           '菱形肌',     'BACK',     4),
('MUS0005', 'DELTOID_FRONT',      '三角肌前束',  'SHOULDER', 5),
('MUS0006', 'DELTOID_SIDE',       '三角肌中束',  'SHOULDER', 6),
('MUS0007', 'DELTOID_REAR',       '三角肌后束',  'SHOULDER', 7),
('MUS0008', 'BICEPS',             '肱二头肌',   'ARM',      8),
('MUS0009', 'TRICEPS',            '肱三头肌',   'ARM',      9),
('MUS0010', 'FOREARM',            '前臂',       'ARM',      10),
('MUS0011', 'QUADRICEPS',         '股四头肌',   'LEG',      11),
('MUS0012', 'HAMSTRING',          '腘绳肌',     'LEG',      12),
('MUS0013', 'CALF',               '小腿',       'LEG',      13),
('MUS0014', 'GLUTEUS_MAXIMUS',    '臀大肌',     'GLUTE',    14),
('MUS0015', 'GLUTEUS_MEDIUS',     '臀中肌',     'GLUTE',    15),
('MUS0016', 'RECTUS_ABDOMINIS',   '腹直肌',     'CORE',     16),
('MUS0017', 'OBLIQUE',            '腹斜肌',     'CORE',     17),
('MUS0018', 'ERECTOR_SPINAE',     '竖脊肌',     'CORE',     18);
