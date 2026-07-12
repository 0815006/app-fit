# 健身打卡基础数据改造 PRD（全量重铺版）

> **版本**：v2.0  
> **状态**：已完整重检  
> **关联**：[健身打卡功能一期PRD](./健身打卡功能一期PRD.md) | [健身打卡页面改造计划-Web端](./健身打卡页面改造计划-Web端.md)

---

## 一、改造目标与范围

### 1.1 目标

用一套全新的、精细化的基础铺底数据，**完全替换**现有 [V9](java-fit-server/src/main/resources/db/migration/V9__seed_gym_action_library.sql) 中 32 个粗粒度动作 + 18 块肌肉 + 34 条推荐配置。

### 1.2 核心原则

| 原则 | 说明 |
|------|------|
| **表结构不变** | `gym_muscle`、`gym_action`、`gym_equipment`、`gym_action_muscle_rel`、`gym_action_equipment_rel`、`gym_action_recommendation` 六张表结构完全不动 |
| **旧数据全量作废** | V9 种子数据全部 DELETE + 重新 INSERT |
| **肌群大类复用** | `gym_muscle.muscle_group` 即是一级分类，8 大肌群大类编码不变（CHEST/BACK/SHOULDER/ARM/LEG/GLUTE/CORE/CARDIO） |
| **主/辅肌群逻辑保留** | `gym_action_muscle_rel.is_primary` 字段保留，一个动作 1 个主肌群 + N 个辅肌群 |
| **7 卡片布局** | 原有 7 肌群卡片（CHEST/BACK/SHOULDER/ARM/LEG/GLUTE/CORE） + 新增 1 张「有氧/拉伸」卡片（CARDIO） |

### 1.3 影响范围

| 层级 | 影响 |
|------|------|
| 数据库 | 6 张表数据全量更新 |
| 后端 Java | `GROUP_NAME_MAP` 加 `CARDIO` 项；恢复时间修正值自然生效 |
| 前端 Web | `MuscleDashboard.vue` 的 `GROUP_ICONS` / `GROUP_COLORS` 加 `CARDIO` 项 |
| 小程序 | 同理加 `CARDIO` 卡片 |

---

## 二、现有数据模型现状（改造前基线）

### 2.1 现有肌群大类编码与中文名映射

来自 [`GymWorkoutRecordServiceImpl.GROUP_NAME_MAP`](java-fit-server/src/main/java/com/fit/service/impl/GymWorkoutRecordServiceImpl.java:36)：

| 编码 | 中文 | V9 肌肉数量 |
|------|------|------------|
| CHEST | 胸部 | 1（胸大肌） |
| BACK | 背部 | 3（背阔肌/斜方肌/菱形肌） |
| SHOULDER | 肩部 | 3（三角肌前/中/后束） |
| ARM | 手臂 | 3（肱二头肌/肱三头肌/前臂） |
| LEG | 腿部 | 3（股四头肌/腘绳肌/小腿） |
| GLUTE | 臀部 | 2（臀大肌/臀中肌） |
| CORE | 核心 | 3（腹直肌/腹斜肌/竖脊肌） |
| FULL_BODY | 全身 | 0 |
| **CARDIO** | **有氧/拉伸** | **0（新增）** |

### 2.2 现有动作数量

[V9](java-fit-server/src/main/resources/db/migration/V9__seed_gym_action_library.sql) 共 32 个动作：胸部 4 + 背部 4 + 肩部 4 + 手臂 4 + 腿部 4 + 臀部 2 + 核心 4 + 有氧 4 + 拉伸 2。

### 2.3 现有器械数量

[V3](java-fit-server/src/main/resources/db/migration/V3__create_gym_equipment_table.sql) 共 16 种器械（EQU0001 ~ EQU0016）。

---

## 三、gym_muscle 肌群字典改造详细清单

### 3.1 改造策略

- **DELETE** 所有 V4 种子数据（MUS0001 ~ MUS0018）
- **INSERT** 全新的精细化肌肉记录
- `muscle_group` 字段即一级分类，无需额外 `parent_muscle_code` 字段
- `base_recovery_hours` 按 PRD 标准修正

### 3.2 全量肌群数据（新）

#### 胸部 CHEST（3 块）

| ID | muscle_code | muscle_name | muscle_group | sort_no | base_recovery_hours |
|----|-------------|-------------|-------------|---------|---------------------|
| MUS0101 | UPPER_CHEST | 胸上束 | CHEST | 1 | 48 |
| MUS0102 | MID_CHEST | 胸中束 | CHEST | 2 | 48 |
| MUS0103 | LOWER_CHEST | 胸下束 | CHEST | 3 | 48 |

#### 背部 BACK（5 块）

| ID | muscle_code | muscle_name | muscle_group | sort_no | base_recovery_hours |
|----|-------------|-------------|-------------|---------|---------------------|
| MUS0201 | LATISSIMUS_DORSI | 背阔肌 | BACK | 4 | **72**（修正） |
| MUS0202 | TRAPEZIUS | 斜方肌 | BACK | 5 | **72**（修正） |
| MUS0203 | RHOMBOID | 菱形肌 | BACK | 6 | **72**（修正） |
| MUS0204 | TERES_MAJOR_MINOR | 大圆肌/小圆肌 | BACK | 7 | **72**（修正） |
| MUS0205 | ERECTOR_SPINAE | 竖脊肌 | BACK | 8 | **72**（修正，原属 CORE） |

> ⚠️ **数据修正说明**：V21 中 BACK 全部设为 48h，PRD 要求背部整体 72h，此处修正。  
> ⚠️ **归类修正说明**：竖脊肌从 `CORE` 移至 `BACK`，与 PRD 背部动作图谱一致（传统硬拉、罗马尼亚硬拉、山羊挺身均属背部）。

#### 肩部 SHOULDER（3 块）

| ID | muscle_code | muscle_name | muscle_group | sort_no | base_recovery_hours |
|----|-------------|-------------|-------------|---------|---------------------|
| MUS0301 | DELTOID_FRONT | 三角肌前束 | SHOULDER | 9 | **48**（修正） |
| MUS0302 | DELTOID_SIDE | 三角肌中束 | SHOULDER | 10 | **48**（修正） |
| MUS0303 | DELTOID_REAR | 三角肌后束 | SHOULDER | 11 | **48**（修正） |

> ⚠️ **数据修正说明**：V21 中 SHOULDER 全部设为 24h，PRD 要求肩部 48h，此处修正。

#### 手臂 ARM（3 块）

| ID | muscle_code | muscle_name | muscle_group | sort_no | base_recovery_hours |
|----|-------------|-------------|-------------|---------|---------------------|
| MUS0401 | BICEPS | 肱二头肌 | ARM | 12 | 24 |
| MUS0402 | TRICEPS | 肱三头肌 | ARM | 13 | 24 |
| MUS0403 | FOREARM | 前臂肌群 | ARM | 14 | 24 |

#### 腿部 LEG（3 块）

| ID | muscle_code | muscle_name | muscle_group | sort_no | base_recovery_hours |
|----|-------------|-------------|-------------|---------|---------------------|
| MUS0501 | QUADRICEPS | 股四头肌 | LEG | 15 | 72 |
| MUS0502 | HAMSTRING | 腘绳肌 | LEG | 16 | 72 |
| MUS0503 | CALF | 小腿三头肌 | LEG | 17 | 72 |

#### 臀部 GLUTE（2 块）

| ID | muscle_code | muscle_name | muscle_group | sort_no | base_recovery_hours |
|----|-------------|-------------|-------------|---------|---------------------|
| MUS0601 | GLUTEUS_MAXIMUS | 臀大肌 | GLUTE | 18 | 48 |
| MUS0602 | GLUTEUS_MEDIUS | 臀中肌 | GLUTE | 19 | 48 |

> ℹ️ **说明**：PRD 概念上把臀大肌归入"腿部肌群"章节，但数据库中 GLUTE 是独立的 `muscle_group`，前端有独立的臀部卡片。保留独立 GLUTE 卡片不变，训练时选臀部动作仍然走 GLUTE 通道。

#### 核心 CORE（3 块）

| ID | muscle_code | muscle_name | muscle_group | sort_no | base_recovery_hours |
|----|-------------|-------------|-------------|---------|---------------------|
| MUS0701 | RECTUS_ABDOMINIS | 腹直肌 | CORE | 20 | 24 |
| MUS0702 | OBLIQUE | 腹斜肌 | CORE | 21 | 24 |
| MUS0703 | DEEP_CORE | 核心深层肌群 | CORE | 22 | 24 |

> ℹ️ **新增**：`DEEP_CORE`（腹横肌等核心深层稳定肌群），对应平板支撑、健腹轮推行等动作。

#### 有氧/拉伸 CARDIO（1 块，新增）

| ID | muscle_code | muscle_name | muscle_group | sort_no | base_recovery_hours |
|----|-------------|-------------|-------------|---------|---------------------|
| MUS0801 | CARDIO_GENERAL | 有氧/拉伸 | CARDIO | 23 | 24 |

> ℹ️ **新增**：`CARDIO` 作为第 7+1 张独立卡片。有氧动作恢复时间短（24h），主要用于记录跑步、椭圆机、动感单车等。

### 3.3 恢复时间修正汇总

| muscle_group | V21 旧值 | 新值 | 依据 |
|-------------|---------|------|------|
| CHEST | 48h | **48h** | 不变 |
| BACK | 48h | **72h** | PRD 要求 72h |
| SHOULDER | 24h | **48h** | PRD 要求 48h |
| ARM | 24h | **24h** | 不变 |
| LEG | 72h | **72h** | 不变 |
| GLUTE | 48h | **48h** | 不变 |
| CORE | 24h | **24h** | 不变 |
| CARDIO | — | **24h** | 新增 |

---

## 四、gym_equipment 器械字典改造详细清单

### 4.1 改造策略

- **保留** V3 现有 16 种器械（EQU0001 ~ EQU0016）
- **追加** PRD 动作图谱中需要但缺失的新器械

### 4.2 待追加器械

| ID | equipment_code | equipment_name | equipment_type |
|----|---------------|----------------|----------------|
| EQU0017 | EZ_BAR | EZ曲柄杠铃 | FREE_WEIGHT |
| EQU0018 | PREACHER_BENCH | 牧师凳 | MACHINE |
| EQU0019 | FLAT_BENCH | 平凳 | FREE_WEIGHT |
| EQU0020 | INCLINE_BENCH | 上斜凳 | FREE_WEIGHT |
| EQU0021 | DECLINE_BENCH | 下斜凳 | FREE_WEIGHT |
| EQU0022 | HYPEREXTENSION_BENCH | 45度挺身架（罗马椅） | MACHINE |
| EQU0023 | SQUAT_RACK | 深蹲架 | MACHINE |
| EQU0024 | LEG_CURL_MACHINE | 腿弯举机 | MACHINE |
| EQU0025 | HACK_SQUAT_MACHINE | 哈克深蹲机 | MACHINE |
| EQU0026 | HIP_ADDUCTOR_MACHINE | 夹腿机（外展/内收） | MACHINE |
| EQU0027 | CALF_RAISE_MACHINE | 提踵机 | MACHINE |
| EQU0028 | YOGA_MAT | 瑜伽垫 | FUNCTIONAL |
| EQU0029 | AB_WHEEL | 健腹轮 | FUNCTIONAL |
| EQU0030 | MEDICINE_BALL | 药球 | FUNCTIONAL |
| EQU0031 | HAMMER_STRENGTH | 悍马机（通用固定器械） | MACHINE |
| EQU0032 | LATERAL_RAISE_MACHINE | 侧平举专用机 | MACHINE |
| EQU0033 | GLUTE_DRIVE_MACHINE | 臀推机 | MACHINE |
| EQU0034 | ANKLE_STRAP | 脚踝绑带 | CABLE |

---

## 五、gym_action 动作标准库全量替换清单

### 5.1 改造策略

- **DELETE** V9 全部 32 条动作数据
- **INSERT** 以下全量动作（按肌群分组）
- 沿用原有 `action_type`、`movement_pattern`、`difficulty_level` 等字段

### 5.2 胸部动作（Chest / 17 个）

| ID | name | alias | action_type | difficulty | 主肌群(muscle_code) |
|----|------|-------|------------|------------|---------------------|
| ACT1001 | 上斜杠铃卧推 | Incline Barbell Bench Press | COMPOUND | 2 | UPPER_CHEST |
| ACT1002 | 上斜哑铃卧推 | Incline Dumbbell Press | COMPOUND | 2 | UPPER_CHEST |
| ACT1003 | 上斜哑铃飞鸟 | Incline Dumbbell Fly | ISOLATION | 2 | UPPER_CHEST |
| ACT1004 | 史密斯上斜卧推 | Smith Incline Press | COMPOUND | 1 | UPPER_CHEST |
| ACT1005 | 绳索低位上拉夹胸 | Low Cable Crossover | ISOLATION | 2 | UPPER_CHEST |
| ACT1006 | 悍马机上斜推胸 | Hammer Strength Incline Press | COMPOUND | 1 | UPPER_CHEST |
| ACT1007 | 平板杠铃卧推 | Barbell Bench Press | COMPOUND | 2 | MID_CHEST |
| ACT1008 | 平板哑铃卧推 | Dumbbell Bench Press | COMPOUND | 2 | MID_CHEST |
| ACT1009 | 坐姿推胸机推胸 | Chest Press Machine | ISOLATION | 1 | MID_CHEST |
| ACT1010 | 蝴蝶机夹胸 | Pec Deck Fly | ISOLATION | 1 | MID_CHEST |
| ACT1011 | 绳索中位水平夹胸 | Cable Crossover | ISOLATION | 2 | MID_CHEST |
| ACT1012 | 平板哑铃飞鸟 | Flat Dumbbell Fly | ISOLATION | 2 | MID_CHEST |
| ACT1013 | 双杠臂屈伸（胸肌版） | Chest Dip | COMPOUND | 3 | LOWER_CHEST |
| ACT1014 | 下斜杠铃卧推 | Decline Barbell Bench Press | COMPOUND | 2 | LOWER_CHEST |
| ACT1015 | 下斜哑铃卧推 | Decline Dumbbell Press | COMPOUND | 2 | LOWER_CHEST |
| ACT1016 | 绳索高位下压夹胸 | High Cable Crossover | ISOLATION | 2 | LOWER_CHEST |
| ACT1017 | 悍马机下斜推胸 | Hammer Strength Decline Press | COMPOUND | 1 | LOWER_CHEST |

### 5.3 背部动作（Back / 14 个）

| ID | name | alias | action_type | difficulty | 主肌群(muscle_code) |
|----|------|-------|------------|------------|---------------------|
| ACT2001 | 正手高位下拉 | Wide Grip Lat Pulldown | COMPOUND | 1 | LATISSIMUS_DORSI |
| ACT2002 | 宽握引体向上 | Wide Grip Pull Up | COMPOUND | 3 | LATISSIMUS_DORSI |
| ACT2003 | 反握窄握高位下拉 | Reverse Grip Lat Pulldown | COMPOUND | 1 | LATISSIMUS_DORSI |
| ACT2004 | 直臂下压 | Straight Arm Pulldown | ISOLATION | 2 | LATISSIMUS_DORSI |
| ACT2005 | 单臂哑铃划船 | One-Arm Dumbbell Row | COMPOUND | 2 | LATISSIMUS_DORSI |
| ACT2006 | 悍马机单臂下拉 | Hammer Strength Unilateral Pulldown | COMPOUND | 1 | LATISSIMUS_DORSI |
| ACT2007 | 俯身杠铃划船 | Barbell Bent Over Row | COMPOUND | 3 | TRAPEZIUS |
| ACT2008 | 坐姿绳索划船 | Seated Cable Row | COMPOUND | 1 | TRAPEZIUS |
| ACT2009 | T杠划船 | T-Bar Row | COMPOUND | 2 | TRAPEZIUS |
| ACT2011 | 蝴蝶机反向飞鸟 | Reverse Pec Deck | ISOLATION | 1 | TRAPEZIUS |
| ACT2012 | 哑铃耸肩 | Dumbbell Shrug | ISOLATION | 1 | TRAPEZIUS |
| ACT2013 | 传统硬拉 | Conventional Deadlift | COMPOUND | 3 | ERECTOR_SPINAE |
| ACT2014 | 罗马尼亚硬拉 | Romanian Deadlift | COMPOUND | 2 | ERECTOR_SPINAE |
| ACT2015 | 山羊挺身 | Hyperextension | ISOLATION | 1 | ERECTOR_SPINAE |

> ⚠️ **删除说明**：原 ACT2010「哑铃俯身飞鸟（背）」与 ACT4010「哑铃俯身侧平举」英文别名完全相同（`Dumbbell Bent Over Lateral Raise`），在解剖学上该动作公认属于三角肌后束（肩部），归入背部/斜方肌不准确。删除后背部仍有 ACT2011 蝴蝶机反向飞鸟作为斜方肌孤立动作。

### 5.4 腿部与臀部动作（Legs & Glutes / 15 个）

| ID | name | alias | action_type | difficulty | 主肌群(muscle_code) |
|----|------|-------|------------|------------|---------------------|
| ACT3001 | 杠铃高位深蹲 | Barbell Back Squat | COMPOUND | 3 | QUADRICEPS |
| ACT3002 | 倒蹬机推举 | Leg Press | COMPOUND | 1 | QUADRICEPS |
| ACT3003 | 坐姿腿屈伸 | Leg Extension | ISOLATION | 1 | QUADRICEPS |
| ACT3004 | 哈克深蹲 | Hack Squat | COMPOUND | 2 | QUADRICEPS |
| ACT3005 | 哑铃向后箭步蹲 | Dumbbell Reverse Lunge | COMPOUND | 2 | QUADRICEPS |
| ACT3006 | 俯卧腿弯举 | Lying Leg Curl | ISOLATION | 1 | HAMSTRING |
| ACT3007 | 坐姿腿弯举 | Seated Leg Curl | ISOLATION | 1 | HAMSTRING |
| ACT3008 | 直腿硬拉 | Stiff Leg Deadlift | COMPOUND | 2 | HAMSTRING |
| ACT3009 | 杠铃臀推 | Barbell Hip Thrust | COMPOUND | 2 | GLUTEUS_MAXIMUS |
| ACT3010 | 史密斯臀推 | Smith Hip Thrust | COMPOUND | 1 | GLUTEUS_MAXIMUS |
| ACT3011 | 绳索后踢腿 | Cable Kickback | ISOLATION | 1 | GLUTEUS_MAXIMUS |
| ACT3012 | 保加利亚单腿蹲 | Bulgarian Split Squat | COMPOUND | 2 | GLUTEUS_MAXIMUS |
| ACT3013 | 坐姿器械外展 | Seated Hip Abduction | ISOLATION | 1 | GLUTEUS_MEDIUS |
| ACT3014 | 站姿杠铃提踵 | Barbell Standing Calf Raise | ISOLATION | 1 | CALF |
| ACT3015 | 坐姿器械提踵 | Seated Calf Raise | ISOLATION | 1 | CALF |

> ℹ️ **新增**：ACT3014/ACT3015 小腿提踵动作，结束小腿三头肌（MUS0503）无专属动作的问题。

> ℹ️ **归类说明**：PRD 将臀大肌归入"腿部"章节，但数据库中 GLUTE 是独立 `muscle_group`。此处的臀推类动作 `muscle_group` 仍走 GLUTE 通道，前端显示在臀部卡片下。用户训练时选腿部或臀部卡片均可看到这些动作（通过辅肌群关联）。

### 5.5 肩部动作（Shoulders / 13 个）

| ID | name | alias | action_type | difficulty | 主肌群(muscle_code) |
|----|------|-------|------------|------------|---------------------|
| ACT4001 | 坐姿哑铃推举 | Seated Dumbbell Shoulder Press | COMPOUND | 2 | DELTOID_FRONT |
| ACT4002 | 杠铃推举 | Barbell Overhead Press | COMPOUND | 3 | DELTOID_FRONT |
| ACT4003 | 史密斯坐姿推举 | Smith Shoulder Press | COMPOUND | 1 | DELTOID_FRONT |
| ACT4004 | 哑铃前平举 | Dumbbell Front Raise | ISOLATION | 1 | DELTOID_FRONT |
| ACT4005 | 绳索前平举 | Cable Front Raise | ISOLATION | 1 | DELTOID_FRONT |
| ACT4006 | 哑铃侧平举 | Dumbbell Lateral Raise | ISOLATION | 1 | DELTOID_SIDE |
| ACT4007 | 绳索单臂侧平举 | Cable Lateral Raise | ISOLATION | 1 | DELTOID_SIDE |
| ACT4008 | 杠铃直立划船 | Barbell Upright Row | COMPOUND | 2 | DELTOID_SIDE |
| ACT4009 | 器械侧平举 | Machine Lateral Raise | ISOLATION | 1 | DELTOID_SIDE |
| ACT4010 | 哑铃俯身侧平举 | Dumbbell Bent Over Lateral Raise | ISOLATION | 2 | DELTOID_REAR |
| ACT4011 | 蝴蝶机反向飞鸟（肩） | Reverse Pec Deck (Rear Delt) | ISOLATION | 1 | DELTOID_REAR |
| ACT4012 | 绳索面拉 | Cable Face Pull | ISOLATION | 1 | DELTOID_REAR |
| ACT4013 | 上斜俯卧哑铃反向飞鸟 | Incline Prone Reverse Fly | ISOLATION | 2 | DELTOID_REAR |

### 5.6 手臂动作（Arms / 13 个）

| ID | name | alias | action_type | difficulty | 主肌群(muscle_code) |
|----|------|-------|------------|------------|---------------------|
| ACT5001 | EZ杆曲柄杠铃弯举 | EZ Bar Curl | ISOLATION | 1 | BICEPS |
| ACT5002 | 哑铃交替弯举 | Alternating Dumbbell Curl | ISOLATION | 1 | BICEPS |
| ACT5003 | 哑铃锤式弯举 | Hammer Curl | ISOLATION | 1 | BICEPS |
| ACT5004 | 牧师凳弯举 | Preacher Curl | ISOLATION | 1 | BICEPS |
| ACT5005 | 绳索低位弯举 | Cable Curl | ISOLATION | 1 | BICEPS |
| ACT5006 | 上斜哑铃弯举 | Incline Dumbbell Curl | ISOLATION | 1 | BICEPS |
| ACT5007 | 绳索下压 | Cable Triceps Pushdown | ISOLATION | 1 | TRICEPS |
| ACT5008 | 窄握杠铃卧推 | Close Grip Bench Press | COMPOUND | 2 | TRICEPS |
| ACT5009 | 仰卧杠铃臂屈伸 | Skull Crusher | ISOLATION | 2 | TRICEPS |
| ACT5010 | 哑铃颈后臂屈伸 | Overhead Dumbbell Extension | ISOLATION | 1 | TRICEPS |
| ACT5011 | 双杠臂屈伸（三头版） | Triceps Dip | COMPOUND | 3 | TRICEPS |
| ACT5012 | 绳索颈后过顶臂屈伸 | Cable Overhead Extension | ISOLATION | 1 | TRICEPS |
| ACT5013 | 杠铃正反握腕弯举 | Barbell Wrist Curl | ISOLATION | 1 | FOREARM |

### 5.7 核心动作（Core / 10 个）

| ID | name | alias | action_type | difficulty | 主肌群(muscle_code) |
|----|------|-------|------------|------------|---------------------|
| ACT6001 | 基础卷腹 | Crunch | ISOLATION | 1 | RECTUS_ABDOMINIS |
| ACT6002 | 仰卧抬腿 | Leg Raise | ISOLATION | 1 | RECTUS_ABDOMINIS |
| ACT6003 | 悬垂举腿 | Hanging Leg Raise | ISOLATION | 3 | RECTUS_ABDOMINIS |
| ACT6004 | 绳索跪姿卷腹 | Cable Kneeling Crunch | ISOLATION | 2 | RECTUS_ABDOMINIS |
| ACT6005 | 健腹轮卷腹 | Ab Wheel Rollout | ISOLATION | 2 | RECTUS_ABDOMINIS |
| ACT6006 | 俄罗斯转体 | Russian Twist | ISOLATION | 1 | OBLIQUE |
| ACT6007 | 负重侧屈 | Weighted Side Bend | ISOLATION | 1 | OBLIQUE |
| ACT6008 | 绳索伐木式转体 | Cable Woodchop | ISOLATION | 2 | OBLIQUE |
| ACT6009 | 平板支撑 | Plank | ISOLATION | 1 | DEEP_CORE |
| ACT6010 | 健腹轮推行 | Ab Wheel Rollout (Full) | ISOLATION | 3 | DEEP_CORE |

### 5.8 有氧/拉伸动作（Cardio / 7 个）

| ID | name | alias | action_type | difficulty | 主肌群(muscle_code) |
|----|------|-------|------------|------------|---------------------|
| ACT7001 | 跑步机慢跑 | Treadmill Run | CARDIO | 1 | CARDIO_GENERAL |
| ACT7002 | 椭圆机太空漫步 | Elliptical Workout | CARDIO | 1 | CARDIO_GENERAL |
| ACT7003 | 划船机有氧 | Rowing Machine Workout | CARDIO | 2 | CARDIO_GENERAL |
| ACT7004 | 动感单车间歇骑行 | Spinning Bike | CARDIO | 1 | CARDIO_GENERAL |
| ACT7005 | 壶铃摇摆（有氧） | Kettlebell Swing | CARDIO | 2 | CARDIO_GENERAL |
| ACT7006 | TRX胸部拉伸 | TRX Chest Stretch | STRETCH | 1 | CARDIO_GENERAL |
| ACT7007 | 泡沫轴背部滚揉 | Foam Roller Back Rolling | MOBILITY | 1 | CARDIO_GENERAL |

> ℹ️ **说明**：原 V9 中有氧动作分别锚定到 `MUS0013`(小腿)、`MUS0011`(股四头肌) 等，不合理。新方案统一归入 `CARDIO` 肌群大类，作为独立的第 7+1 张卡片展示。

---

## 六、gym_action_muscle_rel 动作-肌群关联规范

### 6.1 关联逻辑（不变）

沿用现有 [`gym_action_muscle_rel`](java-fit-server/src/main/resources/db/migration/V6__create_gym_action_muscle_rel_table.sql) 表结构：

- 每个动作 **必须** 有至少 1 条 `is_primary = 1` 的主肌群关联
- 每个动作 **可选** 有 N 条 `is_primary = 0` 的辅肌群关联
- 后端 [`startWorkout()`](java-fit-server/src/main/java/com/fit/service/impl/GymWorkoutRecordServiceImpl.java:48) 通过 `is_primary=1` 的关联推导 `muscle_group`

### 6.2 主肌群映射规则

每个动作的主肌群（`is_primary=1`）必须指向上述第 5 节表格中「主肌群(muscle_code)」列对应的 `muscle_id`。

**示例**：

```
-- 平板杠铃卧推(ACT1007) → 主肌群: 胸中束(MUS0102)
INSERT INTO gym_action_muscle_rel (id, action_id, muscle_id, is_primary)
VALUES ('AMR0107', 'ACT1007', 'MUS0102', 1);
```

### 6.3 辅肌群推荐策略

参照 V9 的辅肌群关联模式，为每个复合动作添加常见辅肌群。典型示例：

| 动作 | 主肌群 | 建议辅肌群 |
|------|--------|-----------|
| 平板杠铃卧推 | MID_CHEST | DELTOID_FRONT, TRICEPS |
| 宽握引体向上 | LATISSIMUS_DORSI | BICEPS |
| 杠铃高位深蹲 | QUADRICEPS | GLUTEUS_MAXIMUS, HAMSTRING |
| 杠铃推举 | DELTOID_FRONT | TRICEPS, DELTOID_SIDE |
| EZ杆弯举 | BICEPS | FOREARM |
| 双杠臂屈伸（胸肌版） | LOWER_CHEST | TRICEPS |
| 双杠臂屈伸（三头版） | TRICEPS | LOWER_CHEST |
| 传统硬拉 | ERECTOR_SPINAE | HAMSTRING, GLUTEUS_MAXIMUS, TRAPEZIUS |
| 杠铃臀推 | GLUTEUS_MAXIMUS | HAMSTRING |
| 窄握杠铃卧推 | TRICEPS | MID_CHEST |
| 杠铃直立划船 | DELTOID_SIDE | TRAPEZIUS |
| 保加利亚单腿蹲 | GLUTEUS_MAXIMUS | QUADRICEPS |

### 6.4 跨卡片关联（重要）

部分动作的主肌群归入 A 大类，但辅肌群关联到 B 大类。这样用户从 B 卡片进入时也能看到该动作。

**典型场景**：
- 双杠臂屈伸（胸肌版）主肌群 = `LOWER_CHEST`(CHEST)，辅肌群 = `TRICEPS`(ARM)
  → 从胸部卡片和手臂卡片都能搜到
- 臀推类动作主肌群 = `GLUTEUS_MAXIMUS`(GLUTE)，辅肌群 = `HAMSTRING`(LEG)
  → 从臀部卡片和腿部卡片都能搜到

这正是现有 [`GymActionServiceImpl.listByMuscleGroup()`](java-fit-server/src/main/java/com/fit/service/impl/GymActionServiceImpl.java:80) 的逻辑：通过 `gym_muscle.muscle_group` → 所有 `muscle_id` → 所有关联 `action_id`，天然支持跨卡片检索。

---

## 七、gym_action_equipment_rel 动作-器械关联规范

### 7.1 关联逻辑（不变）

沿用 [`gym_action_equipment_rel`](java-fit-server/src/main/resources/db/migration/V7__create_gym_action_equipment_rel_table.sql) 表结构，每个动作可以关联 1~N 个器械。

### 7.2 关联规则

每个动作必须关联至少 1 个器械。以下列出关键映射（完整映射在 Flyway 脚本中实现）：

| 动作 | 关联器械 |
|------|---------|
| 平板杠铃卧推 | 杠铃(EQU0001), 平凳(EQU0019) |
| 上斜哑铃卧推 | 哑铃(EQU0002), 上斜凳(EQU0020) |
| 蝴蝶机夹胸 | 推胸机(EQU0006) |
| 绳索面拉 | 龙门架(EQU0007), 双头绳索 |
| 杠铃高位深蹲 | 杠铃(EQU0001), 深蹲架(EQU0023) |
| 倒蹬机推举 | 腿举机(EQU0005) |
| 山羊挺身 | 45度挺身架(EQU0022) |
| 牧师凳弯举 | EZ曲柄杠铃(EQU0017), 牧师凳(EQU0018) |
| 基础卷腹 | 瑜伽垫(EQU0028) |
| 健腹轮卷腹 | 健腹轮(EQU0029) |
| 跑步机慢跑 | 跑步机(EQU0013) |
| TRX胸部拉伸 | TRX训练带(EQU0011) |

---

## 八、gym_action_recommendation 训练建议配置规范

### 8.1 配置逻辑（不变）

沿用 [`gym_action_recommendation`](java-fit-server/src/main/resources/db/migration/V8__create_gym_action_recommendation_table.sql) 表结构。每个动作按训练目标提供组数/次数/休息时间建议。

### 8.2 配置策略

- **增肌（HYPERTROPHY）**：为所有力量训练动作配置（复合/孤立），3-5组，8-15次，组休 60-90s
- **力量（STRENGTH）**：仅为核心复合动作配置（深蹲/卧推/硬拉/推举/引体），4-6组，1-5次，组休 120-180s
- **减脂（FAT_LOSS）**：为所有有氧类动作配置，持续 30-60min 或 HIIT 模式

### 8.3 动作数与推荐配置数预估

| 肌群 | 动作数 | 预测推荐配置数 |
|------|--------|---------------|
| 胸部 | 17 | 20（约 3 个动作加 STRENGTH 配置） |
| 背部 | 14 | 17（约 3 个动作加 STRENGTH 配置） |
| 腿部+臀部 | 15 | 18（约 3 个动作加 STRENGTH 配置 + 2 个小腿动作） |
| 肩部 | 13 | 14（约 1 个动作加 STRENGTH 配置） |
| 手臂 | 13 | 13（全部 HYPERTROPHY） |
| 核心 | 10 | 10（全部 HYPERTROPHY，平板支撑用 STRENGTH 秒数模式） |
| 有氧/拉伸 | 7 | 7（FAT_LOSS 模式） |
| **合计** | **89** | **99** |

### 8.4 特殊配置说明

**平板支撑**、**健腹轮推行**等静态/自重动作：
- `training_goal = STRENGTH`
- `min_reps` / `max_reps` 表示持续时间秒数（如 60-120s）
- `intensity_tips` 注明"秒数为持续时间"

**拉伸/筋膜放松**动作（ACT7006、ACT7007）：
- `training_goal = FAT_LOSS`
- `min_sets = 1, max_sets = 1`
- `min_reps` / `max_reps` 表示持续时间秒数（如 30-120s）
- `recommend_rest_time = 0`（无组间休息）
- `intensity_tips` 注明"秒数为持续时间"

**跑步机/椭圆机/动感单车**等有氧动作：
- `training_goal = FAT_LOSS`
- `min_sets = 1, max_sets = 1`
- `min_reps / max_reps` 表示持续分钟数（如 30-60）
- `recommend_rest_time = 0`（无组间休息）

### 8.5 逐动作推荐数据明细

> 每条记录对应 `gym_action_recommendation` 表 INSERT 的一行。格式：`(id, action_id, training_goal, min_sets, max_sets, min_reps, max_reps, recommend_rest_time, intensity_tips)`

#### 胸部动作推荐（17 动作 → 20 条）

| rec_id | action_id | goal | min_sets | max_sets | min_reps | max_reps | rest(秒) | intensity_tips |
|--------|-----------|------|----------|----------|----------|----------|----------|----------------|
| REC1001 | ACT1001 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 上斜角度30°~45°，下放至锁骨位置，感受上胸挤压 |
| REC1002 | ACT1001 | STRENGTH | 4 | 5 | 1 | 5 | 180 | 大重量冲击上胸，建议有人保护 |
| REC1003 | ACT1002 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 哑铃自由度更高，强调下放时胸肌充分拉伸 |
| REC1004 | ACT1003 | HYPERTROPHY | 3 | 4 | 10 | 15 | 60 | 孤立动作，用轻重量找准上胸发力感 |
| REC1005 | ACT1004 | HYPERTROPHY | 3 | 4 | 8 | 12 | 75 | 史密斯机固定轨迹，适合上胸稳定冲击大重量 |
| REC1006 | ACT1005 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 绳索低位向上拉，顶峰收缩停顿1秒 |
| REC1007 | ACT1006 | HYPERTROPHY | 3 | 4 | 8 | 12 | 75 | 悍马机单侧发力，弥补上胸不对称 |
| REC1008 | ACT1007 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 使用70%-80% 1RM重量，控制下放速度 |
| REC1009 | ACT1007 | STRENGTH | 4 | 6 | 1 | 5 | 180 | 冲击大重量，务必安排专人保护 |
| REC1010 | ACT1008 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 注意下放时对胸中束的撕裂感 |
| REC1011 | ACT1009 | HYPERTROPHY | 4 | 4 | 10 | 15 | 60 | 适合新手或作为力竭后的补刀动作 |
| REC1012 | ACT1010 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 蝴蝶机夹胸，双臂微屈，充分挤压胸中缝 |
| REC1013 | ACT1011 | HYPERTROPHY | 3 | 4 | 10 | 15 | 60 | 绳索中位交叉夹胸，顶峰停顿挤压 |
| REC1014 | ACT1012 | HYPERTROPHY | 3 | 4 | 10 | 15 | 60 | 平板飞鸟，哑铃下放时手肘微屈，胸肌充分拉伸 |
| REC1015 | ACT1013 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 身体微前倾，偏向练胸下缘；身体垂直偏三头 |
| REC1016 | ACT1014 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 下斜角度15°~20°，感受胸下缘发力 |
| REC1017 | ACT1015 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 哑铃下斜推，自由轨迹刺激下胸轮廓 |
| REC1018 | ACT1016 | HYPERTROPHY | 3 | 4 | 10 | 15 | 60 | 绳索高位向下夹，意念集中在胸下缘 |
| REC1019 | ACT1017 | HYPERTROPHY | 3 | 4 | 8 | 12 | 75 | 悍马机固定轨迹，安全冲击下胸 |
| REC1020 | ACT1014 | STRENGTH | 4 | 5 | 3 | 6 | 180 | 大重量下斜卧推，建议有人保护，专注下胸力量 |

#### 背部动作推荐（14 动作 → 17 条）

| rec_id | action_id | goal | min_sets | max_sets | min_reps | max_reps | rest(秒) | intensity_tips |
|--------|-----------|------|----------|----------|----------|----------|----------|----------------|
| REC2001 | ACT2001 | HYPERTROPHY | 4 | 4 | 10 | 12 | 75 | 拉到最低点时顶峰收缩停顿1秒 |
| REC2002 | ACT2002 | HYPERTROPHY | 3 | 4 | 6 | 12 | 90 | 自重动作，做不到6次可使用弹力带辅助 |
| REC2003 | ACT2002 | STRENGTH | 4 | 5 | 3 | 5 | 180 | 负重引体，腰间挂片增加难度 |
| REC2004 | ACT2003 | HYPERTROPHY | 3 | 4 | 10 | 12 | 75 | 反握窄握更刺激背阔肌下部 |
| REC2005 | ACT2004 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 直臂下压，双臂保持微屈，感受背阔肌拉伸 |
| REC2006 | ACT2005 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 单臂哑铃划船，肘部贴紧躯干向后上方拉 |
| REC2007 | ACT2006 | HYPERTROPHY | 3 | 4 | 8 | 12 | 75 | 悍马机单侧下拉，弥补背部不对称 |
| REC2008 | ACT2007 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 俯身稳定，严禁借助腰部力量猛甩 |
| REC2009 | ACT2007 | STRENGTH | 4 | 5 | 3 | 6 | 180 | 大重量俯身划船，核心收紧，腰带辅助 |
| REC2010 | ACT2008 | HYPERTROPHY | 3 | 5 | 10 | 12 | 60 | 专注于背部中间拉紧与松开的控制 |
| REC2011 | ACT2009 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | T杠划船，胸靠垫减少腰部压力 |
| REC2012 | ACT2011 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 蝴蝶机反向飞鸟，专注后束/斜方肌中下部 |
| REC2013 | ACT2012 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 哑铃耸肩，大重量短行程，顶点停顿 |
| REC2014 | ACT2013 | HYPERTROPHY | 3 | 4 | 6 | 10 | 120 | 腰部挺直，杠铃贴小腿上下，切勿弓背 |
| REC2015 | ACT2013 | STRENGTH | 4 | 5 | 1 | 5 | 180 | 冲击极限硬拉，佩戴腰带和助力带 |
| REC2016 | ACT2014 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 杠铃不过膝，专注腘绳肌和竖脊肌的拉伸 |
| REC2017 | ACT2015 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 山羊挺身，抬起时身体呈一条直线，切勿过伸 |

#### 腿部与臀部动作推荐（15 动作 → 18 条）

| rec_id | action_id | goal | min_sets | max_sets | min_reps | max_reps | rest(秒) | intensity_tips |
|--------|-----------|------|----------|----------|----------|----------|----------|----------------|
| REC3001 | ACT3001 | HYPERTROPHY | 4 | 4 | 8 | 12 | 120 | 王牌动作，腰背挺直，膝盖方向同脚尖 |
| REC3002 | ACT3001 | STRENGTH | 5 | 6 | 2 | 5 | 180 | 大重量爆发，建议佩戴护腰带 |
| REC3003 | ACT3002 | HYPERTROPHY | 3 | 4 | 10 | 12 | 90 | 安全上重量，脚踏板高位偏臀，低位偏股四 |
| REC3004 | ACT3002 | STRENGTH | 4 | 5 | 3 | 6 | 180 | 大重量倒蹬，仅次于深蹲的腿部力量动作，建议佩戴护腰带 |
| REC3005 | ACT3003 | HYPERTROPHY | 4 | 4 | 12 | 15 | 60 | 孤立动作，顶端停顿充分挤压大腿前侧 |
| REC3006 | ACT3004 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 哈克深蹲，固定轨迹更安全，股四头肌集中发力 |
| REC3007 | ACT3005 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 箭步蹲，每条腿交替，核心收紧保持平衡 |
| REC3008 | ACT3006 | HYPERTROPHY | 4 | 4 | 10 | 12 | 60 | 俯卧腿弯举，顶峰停顿感受腘绳肌收缩 |
| REC3009 | ACT3007 | HYPERTROPHY | 3 | 4 | 10 | 12 | 60 | 坐姿腿弯举，脚踝勾紧，控制下放速度 |
| REC3010 | ACT3008 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 直腿硬拉，膝盖微屈不锁死，杠铃贴腿 |
| REC3011 | ACT3009 | HYPERTROPHY | 4 | 4 | 8 | 12 | 90 | 翘臀神技，顶端夹紧臀部，核心收紧挺髋 |
| REC3012 | ACT3009 | STRENGTH | 4 | 5 | 3 | 6 | 180 | 大重量臀推，建议使用臀推垫或杠铃套 |
| REC3013 | ACT3010 | HYPERTROPHY | 3 | 4 | 8 | 12 | 75 | 史密斯固定轨迹，安全冲击臀大肌 |
| REC3014 | ACT3011 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 绳索后踢腿，慢速控制，感受臀部发力 |
| REC3015 | ACT3012 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 保加利亚单腿蹲，前脚踩实，后脚搭凳 |
| REC3016 | ACT3013 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 坐姿外展，身体前倾30°更刺激臀中肌 |
| REC3017 | ACT3014 | HYPERTROPHY | 4 | 4 | 15 | 20 | 45 | 站姿杠铃提踵，大重量高次数，全程快放慢收 |
| REC3018 | ACT3015 | HYPERTROPHY | 4 | 4 | 15 | 20 | 45 | 坐姿器械提踵，固定轨迹安全刺激小腿三头肌 |

#### 肩部动作推荐（13 动作 → 14 条）

| rec_id | action_id | goal | min_sets | max_sets | min_reps | max_reps | rest(秒) | intensity_tips |
|--------|-----------|------|----------|----------|----------|----------|----------|----------------|
| REC4001 | ACT4001 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 坐姿哑铃推举，核心收紧，推起时手肘不锁死 |
| REC4002 | ACT4002 | HYPERTROPHY | 3 | 4 | 6 | 10 | 120 | 杠铃推举，立姿核心收紧，避免腰椎过伸 |
| REC4003 | ACT4002 | STRENGTH | 4 | 5 | 1 | 5 | 180 | 大重量站姿推举，建议佩戴护腰带 |
| REC4004 | ACT4003 | HYPERTROPHY | 3 | 4 | 8 | 12 | 75 | 史密斯固定轨迹，专注前束发力 |
| REC4005 | ACT4004 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 前平举，哑铃抬至肩高即可，不要借力摆动 |
| REC4006 | ACT4005 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 绳索前平举，恒定张力，前束全程受力 |
| REC4007 | ACT4006 | HYPERTROPHY | 4 | 5 | 12 | 15 | 60 | 小重量高次数，手腕微旋（倒水姿势）刺激中束 |
| REC4008 | ACT4007 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 绳索单臂侧平举，恒定张力刺激中束 |
| REC4009 | ACT4008 | HYPERTROPHY | 3 | 4 | 10 | 12 | 75 | 杠铃直立划船，宽握减少肩关节压力 |
| REC4010 | ACT4009 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 器械侧平举，固定轨迹，安全高效刺激中束 |
| REC4011 | ACT4010 | HYPERTROPHY | 4 | 4 | 12 | 15 | 60 | 俯身侧平举，沉肩，用肩后侧发力，不要怂斜方肌 |
| REC4012 | ACT4011 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 蝴蝶机反向飞鸟，专注后束孤立发力 |
| REC4013 | ACT4012 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 绳索面拉，拉向额头，外旋手腕加强肩袖健康 |
| REC4014 | ACT4013 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 上斜俯卧反向飞鸟，完全孤立三角肌后束 |

#### 手臂动作推荐（13 动作 → 13 条）

| rec_id | action_id | goal | min_sets | max_sets | min_reps | max_reps | rest(秒) | intensity_tips |
|--------|-----------|------|----------|----------|----------|----------|----------|----------------|
| REC5001 | ACT5001 | HYPERTROPHY | 3 | 4 | 10 | 12 | 60 | EZ杆弯举，大臂贴紧躯干，身体不晃，顶端挤压 |
| REC5002 | ACT5002 | HYPERTROPHY | 3 | 4 | 10 | 12 | 60 | 哑铃交替弯举，上举时微微外旋手腕，深度刺激 |
| REC5003 | ACT5003 | HYPERTROPHY | 3 | 4 | 10 | 12 | 60 | 锤式弯举，掌心相对，强化肱肌和前臂 |
| REC5004 | ACT5004 | HYPERTROPHY | 3 | 4 | 10 | 12 | 60 | 牧师凳弯举，严格孤立二头肌，充分拉伸下放 |
| REC5005 | ACT5005 | HYPERTROPHY | 3 | 4 | 10 | 12 | 60 | 绳索低位弯举，恒定张力，二头肌全程受力 |
| REC5006 | ACT5006 | HYPERTROPHY | 3 | 4 | 10 | 12 | 60 | 上斜哑铃弯举，手臂后置，二头肌长头充分拉伸 |
| REC5007 | ACT5007 | HYPERTROPHY | 4 | 4 | 12 | 15 | 60 | 绳索下压，大臂夹紧锁定，手臂完全伸直收缩 |
| REC5008 | ACT5008 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 窄握杠铃卧推，握距与肩同宽，专注三头肌发力 |
| REC5009 | ACT5009 | HYPERTROPHY | 3 | 4 | 10 | 12 | 60 | 颅骨粉碎者，肘部指向天花板，控制下放重量 |
| REC5010 | ACT5010 | HYPERTROPHY | 3 | 4 | 10 | 12 | 60 | 哑铃颈后臂屈伸，单手或双手，三头肌长头拉伸 |
| REC5011 | ACT5011 | HYPERTROPHY | 3 | 4 | 8 | 12 | 90 | 双杠臂屈伸（身体直立版），专注三头肌发力 |
| REC5012 | ACT5012 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 绳索颈后过顶臂屈伸，三头肌长头深度刺激 |
| REC5013 | ACT5013 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 杠铃腕弯举，前臂垫于大腿，仅手腕屈伸 |

#### 核心动作推荐（10 动作 → 10 条）

| rec_id | action_id | goal | min_sets | max_sets | min_reps | max_reps | rest(秒) | intensity_tips |
|--------|-----------|------|----------|----------|----------|----------|----------|----------------|
| REC6001 | ACT6001 | HYPERTROPHY | 4 | 4 | 15 | 20 | 45 | 基础卷腹，腰部贴地，用腹肌发力卷起而非脖子 |
| REC6002 | ACT6002 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 仰卧抬腿，下放时脚不着地，下腹持续紧张 |
| REC6003 | ACT6003 | HYPERTROPHY | 3 | 4 | 10 | 15 | 60 | 悬垂举腿，减少身体晃动，用腹部控制下放 |
| REC6004 | ACT6004 | HYPERTROPHY | 3 | 4 | 12 | 15 | 45 | 绳索跪姿卷腹，绳索置于颈后，腹肌顶峰收缩 |
| REC6005 | ACT6005 | HYPERTROPHY | 3 | 4 | 8 | 12 | 60 | 健腹轮卷腹，慢放快收，核心收紧保护腰椎 |
| REC6006 | ACT6006 | HYPERTROPHY | 3 | 4 | 15 | 20 | 45 | 俄罗斯转体，双脚离地增加难度，控制旋转幅度 |
| REC6007 | ACT6007 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 负重侧屈，手持哑铃慢慢向下，对侧腹斜肌拉伸 |
| REC6008 | ACT6008 | HYPERTROPHY | 3 | 4 | 12 | 15 | 60 | 绳索伐木式，模拟伐木动作，核心旋转发力 |
| REC6009 | ACT6009 | STRENGTH | 3 | 3 | 60 | 120 | 45 | 静态核心抗移动（秒数为持续时间），身体一条直线 |
| REC6010 | ACT6010 | HYPERTROPHY | 3 | 4 | 8 | 12 | 60 | 健腹轮全推行，高级核心动作，力量不足可做半程 |

#### 有氧/拉伸动作推荐（7 动作 → 7 条）

| rec_id | action_id | goal | min_sets | max_sets | min_reps | max_reps | rest(秒) | intensity_tips |
|--------|-----------|------|----------|----------|----------|----------|----------|----------------|
| REC7001 | ACT7001 | FAT_LOSS | 1 | 1 | 30 | 60 | 0 | 心率保持在最大心率60%~70%燃脂区间（分钟为持续时间） |
| REC7002 | ACT7002 | FAT_LOSS | 1 | 1 | 30 | 45 | 0 | 大体重减脂首选，不伤膝盖（分钟为持续时间） |
| REC7003 | ACT7003 | FAT_LOSS | 3 | 5 | 500 | 1000 | 60 | 可使用HIIT模式（米数为距离），全力划行+慢速恢复交替 |
| REC7004 | ACT7004 | FAT_LOSS | 1 | 1 | 30 | 45 | 0 | 跟音乐节奏骑行，阻力适中，高心率燃脂（分钟为持续时间） |
| REC7005 | ACT7005 | FAT_LOSS | 3 | 4 | 20 | 30 | 45 | 壶铃摇摆，髋铰链发力，用臀部荡起而非手臂拉 |
| REC7006 | ACT7006 | FAT_LOSS | 1 | 1 | 30 | 60 | 0 | 训练后拉伸（秒数为持续时间），每个静态姿势保持30~60秒 |
| REC7007 | ACT7007 | FAT_LOSS | 1 | 1 | 60 | 120 | 0 | 训练后滚揉（秒数为持续时间），针对酸痛点缓慢滑行，每个部位1~2分钟 |

> **汇总**：共 99 条推荐配置（胸部20 + 背部17 + 腿部+臀部18 + 肩部14 + 手臂13 + 核心10 + 有氧7）

---

## 九、Flyway 版本规划

### 9.1 版本编排

当前最新版本：[V22](java-fit-server/src/main/resources/db/migration/V22__create_gym_workout_record.sql)

| 版本 | 文件名 | 职责 |
|------|--------|------|
| **V23** | `V23__rebuild_gym_muscle.sql` | 清空 gym_muscle 旧数据，插入新 23 条肌肉记录（含恢复时间修正） |
| **V24** | `V24__extend_gym_equipment.sql` | 追加 EQU0017~EQU0034 共 18 种新器械 |
| **V25** | `V25__rebuild_gym_action.sql` | 清空 gym_action 旧数据，插入新 89 条动作 |
| **V26** | `V26__rebuild_gym_action_muscle_rel.sql` | 清空 gym_action_muscle_rel，插入新动作-肌群关联（~180 条） |
| **V27** | `V27__rebuild_gym_action_equipment_rel.sql` | 清空 gym_action_equipment_rel，插入新动作-器械关联（~120 条） |
| **V28** | `V28__rebuild_gym_action_recommendation.sql` | 清空 gym_action_recommendation，插入新 AI 推荐配置（99 条） |

### 9.2 执行顺序依赖

```
V23 (肌肉) → V24 (器械) → V25 (动作) → V26 (动作-肌肉) → V27 (动作-器械) → V28 (推荐配置)
```

必须严格按上述顺序执行，因为 V26 依赖 V23+V25 的 ID，V27 依赖 V24+V25 的 ID。

### 9.3 旧数据清理策略

| 表 | 清理方式 |
|----|---------|
| gym_muscle | `DELETE FROM gym_muscle` 全量清空后重插 |
| gym_equipment | 保留 EQU0001~EQU0016，追加 EQU0017~EQU0034 |
| gym_action | `DELETE FROM gym_action` 全量清空后重插 |
| gym_action_muscle_rel | `DELETE FROM gym_action_muscle_rel` 全量清空后重插 |
| gym_action_equipment_rel | `DELETE FROM gym_action_equipment_rel` 全量清空后重插 |
| gym_action_recommendation | `DELETE FROM gym_action_recommendation` 全量清空后重插 |

> ⚠️ **注意**：`gym_workout_record`（训练打卡记录）不在此次改造范围，已打卡数据保留不动。其 `action_id` 和 `muscle_group` 字段与旧数据关联，但新的 Dashboard 查询按 `muscle_group` 分组统计，兼容无问题。

---

## 十、前端影响评估

### 10.1 MuscleDashboard.vue 变更

需在 [MuscleDashboard.vue](web-fit-vue/src/components/workout/MuscleDashboard.vue) 的 `GROUP_ICONS` 和 `GROUP_COLORS` 中新增：

```typescript
// GROUP_ICONS 新增
CARDIO: '🏃',

// GROUP_COLORS 新增
CARDIO: '#f39c12',
```

### 10.2 后端 GROUP_NAME_MAP 变更

需在 [GymWorkoutRecordServiceImpl](java-fit-server/src/main/java/com/fit/service/impl/GymWorkoutRecordServiceImpl.java:36) 新增：

```java
GROUP_NAME_MAP.put("CARDIO", "有氧");
```

### 10.3 TrainingStatsServiceImpl 变更

需在 [TrainingStatsServiceImpl](java-fit-server/src/main/java/com/fit/service/impl/TrainingStatsServiceImpl.java:80) 的 `muscleGroups` 数组中新增 `"CARDIO"`：

```java
String[] muscleGroups = {"CHEST", "BACK", "SHOULDER", "ARM", "LEG", "GLUTE", "CORE", "CARDIO"};
```

### 10.4 小程序端

对应的微信小程序前端同样需要在肌群卡片列表中增加 CARDIO 卡片（图标 + 颜色映射）。

---

## 十一、实施检查清单（Checklist）

### 11.1 数据完整性

- [ ] gym_muscle：23 条记录全部插入，sort_no 连续，base_recovery_hours 正确
- [ ] gym_equipment：34 种器械（原 16 + 新 18），equipment_type 正确分类
- [ ] gym_action：89 条动作，action_type / movement_pattern / difficulty_level 正确
- [ ] gym_action_muscle_rel：每个动作至少有 1 条 is_primary=1 的关联
- [ ] gym_action_equipment_rel：每个动作至少关联 1 个器械
- [ ] gym_action_recommendation：每个动作至少有 1 条增肌/力量/减脂配置

### 11.2 恢复时间修正

- [ ] BACK 全部 72h（原 48h）
- [ ] SHOULDER 全部 48h（原 24h）
- [ ] CHEST/ARM/LEG/GLUTE/CORE/CARDIO 与 PRD 一致

### 11.3 归类修正

- [ ] 竖脊肌 `ERECTOR_SPINAE` 从 CORE 移至 BACK
- [ ] 臀大肌/臀中肌保留在 GLUTE（独立卡片）
- [ ] CARDIO 新增为独立 muscle_group

### 11.4 前后端适配

- [ ] 后端 GROUP_NAME_MAP 加 "CARDIO"
- [ ] 后端 TrainingStatsServiceImpl muscleGroups 加 "CARDIO"
- [ ] 前端 MuscleDashboard GROUP_ICONS / GROUP_COLORS 加 CARDIO
- [ ] 小程序端同步加 CARDIO 卡片

### 11.5 回滚方案

Flyway 迁移不可逆。如需回滚，需准备独立的回滚 SQL 脚本（`DELETE FROM` 新数据 + 重新执行 V4/V9 种子脚本）。

---

## 附录 A：muscle_group 编码对照总表

| 编码 | 中文名 | 卡片序号 | 恢复时间 | 二级肌肉数 | 动作数 |
|------|--------|---------|---------|-----------|--------|
| CHEST | 胸部 | 1 | 48h | 3 | 17 |
| BACK | 背部 | 2 | 72h | 5 | 14 |
| SHOULDER | 肩部 | 3 | 48h | 3 | 13 |
| ARM | 手臂 | 4 | 24h | 3 | 13 |
| LEG | 腿部 | 5 | 72h | 3 | 10 |
| GLUTE | 臀部 | 6 | 48h | 2 | 5 |
| CORE | 核心 | 7 | 24h | 3 | 10 |
| CARDIO | 有氧/拉伸 | 8 | 24h | 1 | 7 |
| **合计** | | **8 卡片** | | **23** | **89** |

> ℹ️ LEG（10个）= 股四头肌x5 + 腘绳肌x3 + 小腿x2。GLUTE（5个）= 臀大肌x4 + 臀中肌x1。LEG与GLUTE动作通过辅肌群跨卡片可见。
> ℹ️ 菱形肌(MUS0203)和大圆肌/小圆肌(MUS0204)无专属主肌群动作，仅通过辅肌群关联间接训练（如坐姿划船→菱形肌辅）。
> ℹ️ ARM恢复时间24h（小肌群标准），若做高强度大重量训练（如窄握杠铃卧推、大重量弯举）实际恢复建议48h。

---

## 附录 B：与旧 PRD 差异对照

| 维度 | 旧 PRD (v1) | 新 PRD (v2) |
|------|-----------|-----------|
| 二级肌肉表示 | 未说明 | 明确：gym_muscle 表中每条记录即二级肌肉，muscle_group 即一级 |
| Flyway 版本 | 未规划 | V23~V28 共 6 个迁移脚本 |
| 旧数据存废 | 未明确 | 全量 DELETE + INSERT |
| 主/辅肌群 | 未说明 | 保留 is_primary 逻辑，辅肌群实现跨卡片检索 |
| AI 推荐 | 未覆盖 | 预估 99 条配置，明确各模式策略 |
| 有氧/拉伸 | 无归属 | 独立 CARDIO 卡片（第 8 张） |
| 恢复时间 | 背部72/肩部48 | 与 PRD 一致（修正 V21 偏差） |
| 器械扩展 | 未完整定义 | 18 种新器械，含编码和类型 |
| 前后端变更 | 未涉及 | 明确列出 GROUP_NAME_MAP / TrainingStatsServiceImpl / MuscleDashboard 需改处 |
