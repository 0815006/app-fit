-- Flyway migration V3: Create gym_equipment table + init data
-- Gym equipment dictionary for fitness action library.

CREATE TABLE gym_equipment (
    id              VARCHAR(32)  NOT NULL COMMENT '主键ID',
    equipment_code  VARCHAR(50)  NOT NULL COMMENT '器械编码（如：BARBELL）',
    equipment_name  VARCHAR(100) NOT NULL COMMENT '器械名称（如：杠铃）',
    equipment_type  VARCHAR(30)  NOT NULL COMMENT '器械大类类型（FREE_WEIGHT:自由力量, MACHINE:固定器械, CABLE:绳索器械, BODY_WEIGHT:自重训练, CARDIO_EQUIPMENT:有氧器械, FUNCTIONAL:功能训练器械）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (equipment_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='器械字典';

-- Init data
INSERT INTO gym_equipment (id, equipment_code, equipment_name, equipment_type) VALUES
('EQU0001', 'BARBELL',             '杠铃',         'FREE_WEIGHT'),
('EQU0002', 'DUMBBELL',            '哑铃',         'FREE_WEIGHT'),
('EQU0003', 'KETTLEBELL',          '壶铃',         'FREE_WEIGHT'),
('EQU0004', 'SMITH_MACHINE',       '史密斯机',      'MACHINE'),
('EQU0005', 'LEG_PRESS_MACHINE',   '腿举机',       'MACHINE'),
('EQU0006', 'CHEST_PRESS_MACHINE', '推胸机',       'MACHINE'),
('EQU0007', 'CABLE_MACHINE',       '龙门架',       'CABLE'),
('EQU0008', 'LAT_PULLDOWN',        '高位下拉机',    'MACHINE'),
('EQU0009', 'PULL_UP_BAR',         '单杠',         'BODY_WEIGHT'),
('EQU0010', 'DIP_BAR',             '双杠',         'BODY_WEIGHT'),
('EQU0011', 'TRX',                 'TRX训练带',     'FUNCTIONAL'),
('EQU0012', 'BATTLE_ROPE',         '战绳',         'FUNCTIONAL'),
('EQU0013', 'TREADMILL',           '跑步机',       'CARDIO_EQUIPMENT'),
('EQU0014', 'ELLIPTICAL',          '椭圆机',       'CARDIO_EQUIPMENT'),
('EQU0015', 'ROWING_MACHINE',      '划船机',       'CARDIO_EQUIPMENT'),
('EQU0016', 'BIKE',                '动感单车',     'CARDIO_EQUIPMENT');
