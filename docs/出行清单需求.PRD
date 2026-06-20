# 产品需求文档 (PRD) —— “出行清单 (TripCheck)”智能全场景备忘系统 (Web网页版 V1.0)

## 1. 产品概述与核心价值

* **产品名称**：常备清单 / 出行清单 (TripCheck)
* **产品定位**：一个基于“单页状态机、用户多维标签正交、关系型物品归因、临行极限复检”的网页端智能出行与日常资产备忘系统。
* **核心价值**：通过建立全局基底大库与正交标签规则，覆盖用户日常短途（上班、健身、去医院）与长途长周期（旅游、出差）场景。解决由于外部环境（天气、交通、住宿）变化导致的错带、漏带焦虑，实现“平时多配置、用时秒生成、临行快核对”的轻量化交互闭环。

---

## 2. 系统核心模型架构

### 2.1 上下文标签 (Context Tag) 维度设计

所有影响清单生成的外部时空变量抽象为统一的上下文标签体系，通过 `type` 字段进行横向正交扩展（“天数”作为计划原生参数独立处理，不计入标签库）：

```text
【上下文标签 (Context Tag) 维度】
 ├── SCENARIO (主场景)     --> 日常通勤, 去健身房, 去医院, 长途旅游/出差
 ├── ACTIVITY (特定活动)   --> 游泳, 爬山, 漂流, 沙滩游玩, 做客
 ├── USER_GENDER (性别)    --> 男士专属, 女士专属
 ├── WEATHER (天气环境)    --> 高温, 低温, 雨天, 雪天, 强紫外线
 ├── TRANSPORT (交通方式)  --> 飞机, 高铁, 自驾
 └── ACCOMMODATION (住宿)  --> 酒店, 露营, 朋友家, 民宿

```

### 2.2 用户画像与特异性资产隔离

* **用户专属物品**：用户特定的数码设备或常备药品，直接在物品表中绑定 `user_id`（全局公共物品 `user_id` 为 `NULL`）。
* **隐私安全红线**：**严禁在系统任何地方收集、推导或存储用户的医疗健康隐私信息**（如具体疾病名称等）。如需携带特定药品，直接作为【用户专属物品】维护。

### 3.3 物品重要级别 (Importance Level)

每个物品强制定义重要程度，用以驱动核对视图的渲染权重及临行口袋模式的展示层级：

* `CRITICAL` (致命重要：不带寸步难行，如身份证、手机、钥匙、护照)
* `IMPORTANT` (高度重要：影响核心体验，如充电器、眼镜、必备药品)
* `OPTIONAL` (可选携带：提升舒适度，如耳塞、眼罩、自拍杆)

---

## 3. 功能需求详述

### ⚙️ 模块一：全局基底铺底与血缘关联（底座维护）

* **3.1 关系型多来源血缘归因**
* 摒弃纯 JSON 关联，引入实体关系表 `trip_plan_source`。清单中某项物品被触发的所有场景源均可精确记录，完美支撑后续的场景快捷过滤与多重归因聚合统计。


* **3.2 双轨制模板与冷启动**
* **官方预置模板（只读）**：内置“标准出差”、“正常上班”、“去健身房”、“去医院”等开箱即用数据，解决用户冷启动门槛。
* **用户自定义**：支持个人微调规则的保存与应用。


* **3.3 共享社区预留**
* 数据库结构预留 `is_public` (0/1) 与 `parent_template_id` 字段，为后续版本拓展社区模板市场提供基础。



### 🔀 模块二：多维正交裁剪与表达式数量引擎（“选”的逻辑）

* **4.1 三档初始版本选择**
* **极限轻装版**：系统仅拉取关联关系为 `MINIMAL` 的物品。
* **标准平衡版**：默认拉取 `BASIC` 核心物品，并解锁多维正交标签叠加面板。
* **万能齐全版**：拉取该场景下 `MINIMAL + BASIC + ALL_INCLUSIVE` 的所有物品。


* **4.2 维度矩阵勾选与并集去重**
* 在标准版下，允许用户勾选多个维度标签（如 `[出差]` + `[飞机]` + `[雨天]`）。系统合并所有集合，并基于 `item_id` 进行**全局唯一性去重 (Distinct)**。


* **4.3 进阶数量公式引擎**
* 支持长途场景的天数变量（正整数 $N$）。表达式字段支持标准枚举解析：`FIXED` (数量=1)、`DAY*1` (数量=$N$)、`DAY/2` (向上取整)、`LIMIT_MAX_4` (不超过4件)。



---

## 4. Web 网页版单页状态机页面设计

Web 端采用**单页应用 (SPA) 动态状态机**架构，所有操作收拢于单一页面，通过状态流转（`pageStatus`）实现无缝切换。

```text
[单页状态流转]
  HOME (默认首页：秒开日常 / 发起长途)
   │ 
   ├──> 点击日常卡片 ──────────────────────────┐
   │                                            v
   └──> 填表发起长途 ──> TAILOR (裁剪页) ──> CHECKING (核心核对打包看板)
                                                │
          ┌─────────────────────────────────────┘
          v
  重置 <─ 临行30秒 (口袋模式)

```

### 📱 视图状态 A：【闲置/默认首页】 (Home & Quick-Start)

页面分为上下/左右常驻区，提供极简的点选入口：

* **日常秒开区 (Routine Quick-Start)**：
* 平铺 4 个大卡片：`[🏢 日常通勤]`、`[🏋️ 去健身房]`、`[🏊 去游泳]`、`[🏥 去医院]`。
* **零思考交互**：点击任意卡片，页面不跳转，直接由后端拉取该场景基础配置，视图瞬间原地切换至 **状态 C (核对打包看板)**。


* **远途/专项计划台 (Trip Wizard)**：
* 表单区：输入计划标题、目的地、天数 $N$。
* 选项区：选择精简级别，并勾选多维正交标签（交通、天气、活动等）。
* 按钮：`[✨ 创建专项清单]`。点击后页面直接局部切换至 **状态 B (清单裁剪台)**。



### 🎨 视图状态 B：【清单裁剪与微调台】 (Tailor & Custom)

* *触发条件*：仅在由“专项计划台”创建时显示。
* **规则罗列**：平铺展示系统通过正交矩阵合并去重后生成的初始清单列表。
* **行内微调操作**：
* 数量列提供 `+` / `-` 步进器。
* 每行最右侧提供 `[❌ 临时排除]` 按钮。点击后该行置灰且不计入后续统计（状态可逆），绝对不污染全局基底。
* 底部常驻 `[＋ 手动补漏]` 输入框，随时键入追加临时物品。


* **确认锁定**：按钮 `[🔒 锁定清单，开始核对]`。点击持久化生成 `trip_plan_detail` 实例，视图切换至 **状态 C**。

### 🎒 视图状态 C：【核心核对打包看板】 (Check & Pack)

发挥 PC 大屏优势，将三大物理容器（行李箱/双肩包/随身口袋）以并排列或风琴卡片形式呈现：

* **顶部总览与筛选栏**：
* 实时装箱进度环/进度条（如 `已装箱 24 / 40 件`）。
* **血缘筛选标签墙**（如 `[全部]` `[出差]` `[游泳]` `[雨天]`）。点击徽章，利用血缘关系毫秒级过滤下方列表。
* 关键字模糊搜索框：输入名称实时过滤显示行。


* **中部容器面板（多列布局）**：
* 📦 **【行李箱】列**（大件、托运）
* 🎒 **【双肩包】列**（数码、高频）
* 🧥 **【随身口袋】列**（贴身、贵重）
* **载体动态降级**：若专项计划未配置或不需要行李箱，原行李箱内物品自动无缝归入双肩包列。
* **快捷交互热区**：点击列表任意区域切换装箱状态（`is_checked = 1/0`）。触发时伴随视觉反馈（划掉、文字变灰、自动沉底至当前容器的“已装箱”折叠栏）。


* **底部固定悬浮条**：
* 状态全部打勾完毕后亮起大按钮：`[🚨 开启临行30秒（口袋模式）]`。
* 点击后弹出一个影院级半透明黑色聚焦弹窗：**隐藏所有大件，仅强行展示【随身口袋 POCKET】且重要级别为【CRITICAL】的四件套核心（手机、身份证、钥匙、钱包/工牌）**。用户二次确认打勾，点击 `[妥当，出发！]`，弹窗关闭，页面平滑重置回“状态 A (默认首页)”。



---

## 5. 非功能性与技术约束规范

### 5.1 数据删除安全性

* 删除、归档或重置历史计划实例时，仅物理/逻辑删除明细表（`trip_plan_detail`）及血缘关联表（`trip_plan_source`）的快照数据，**严禁级联删除 (No Cascade Delete)** 底层物品主库与标签规则库。

### 5.2 接口与数据规范

* **接口路径**：后端控制器（Controller）统一以 `/api` 前缀开头。
* **主键类型**：物品 ID、标签 ID、计划实例 ID 均必须采用 **String** 类型主键，确保分布式及后续多端扩展兼容性。
* **关联表索引**：严格通过血缘关联表（`trip_plan_source`）走索引进行场景过滤，规避 JSON 全表扫描引发的性能瓶颈。

---

## 6. 核心数据库建表语句 (MySQL DDL 基准)

```sql
-- 1. 物品主表 (trip_item)
CREATE TABLE `trip_item` (
    `id` VARCHAR(32) NOT NULL COMMENT 'String主键ID',
    `user_id` VARCHAR(32) NULL COMMENT '所属用户ID(NULL代表全局公共物品，隔离隐私)',
    `name` VARCHAR(100) NOT NULL COMMENT '物品名称',
    `category` VARCHAR(50) NOT NULL COMMENT '物品分类(数码、衣物等)',
    `default_container` VARCHAR(32) NOT NULL COMMENT '默认载体(SUITCASE/BACKPACK/POCKET)',
    `importance_level` VARCHAR(20) NOT NULL DEFAULT 'IMPORTANT' COMMENT '重要级别(CRITICAL/IMPORTANT/OPTIONAL)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品主库表';

-- 2. 上下文标签表 (trip_tag)
CREATE TABLE `trip_tag` (
    `id` VARCHAR(32) NOT NULL COMMENT 'String主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '标签名称(如:游泳/雨天/出差)',
    `type` VARCHAR(32) NOT NULL COMMENT '标签维度(SCENARIO/ACTIVITY/USER_GENDER/WEATHER/TRANSPORT/ACCOMMODATION)',
    `is_preset` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否官方预置(1-是,0-用户自建)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上下文标签表';

-- 3. 标签与物品关联规则表 (trip_tag_item)
CREATE TABLE `trip_tag_item` (
    `id` VARCHAR(32) NOT NULL,
    `tag_id` VARCHAR(32) NOT NULL COMMENT '标签ID',
    `item_id` VARCHAR(32) NOT NULL COMMENT '物品ID',
    `version_level` VARCHAR(32) NOT NULL DEFAULT 'BASIC' COMMENT '精简级别(MINIMAL/BASIC/ALL_INCLUSIVE)',
    `multiplier_expr` VARCHAR(128) NOT NULL DEFAULT 'FIXED' COMMENT '数量计算表达式(FIXED/DAY*1/DAY/2/LIMIT_MAX_4)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_item` (`tag_id`, `item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签物品关联规则表';

-- 4. 出行计划实例表 (trip_plan)
CREATE TABLE `trip_plan` (
    `id` VARCHAR(32) NOT NULL,
    `user_id` VARCHAR(32) NOT NULL COMMENT '创建用户ID',
    `title` VARCHAR(150) NOT NULL COMMENT '计划标题(如:8月三亚度假)',
    `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '计划状态(DRAFT/PACKING/FINISHED/ARCHIVED)',
    `trip_days` INT NOT NULL DEFAULT 1 COMMENT '出行天数参数(日常高频场景默认为1)',
    `destination` VARCHAR(100) NULL COMMENT '目的地名称',
    `departure_time` DATETIME NULL COMMENT '出发时间',
    `return_time` DATETIME NULL COMMENT '返回时间',
    `is_public` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否公开分享为社区模板(预留)',
    `parent_template_id` VARCHAR(32) NULL COMMENT '继承的母模板ID(预留)',
    `created_time` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出行计划实例表';

-- 5. 计划物品核对明细表 (trip_plan_detail)
CREATE TABLE `trip_plan_detail` (
    `id` VARCHAR(32) NOT NULL,
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
    `updated_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '服务端最后修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_plan` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划物品核对明细表';

-- 6. 计划物品多重血缘来源表 (trip_plan_source)
CREATE TABLE `trip_plan_source` (
    `id` VARCHAR(32) NOT NULL,
    `plan_detail_id` VARCHAR(32) NOT NULL COMMENT '明细表(trip_plan_detail)外键ID',
    `tag_id` VARCHAR(32) NOT NULL COMMENT '关联触发的上下文标签ID',
    PRIMARY KEY (`id`),
    KEY `idx_plan_detail` (`plan_detail_id`),
    KEY `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划物品多重血缘来源表';

```