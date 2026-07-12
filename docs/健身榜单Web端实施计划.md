# 健身榜单 Web 端实施计划（完整版）

> **目标**：将 Web 端「健康数据」占位页面改造为完整的「健身榜单」功能页面。
>
> **依据**：[健身榜单需求PRD.md](./健身榜单需求PRD.md)
>
> **现状分析**：
> - [`HealthView.vue`](../web-fit-vue/src/views/HealthView.vue) 为空占位页
> - [`RankingBoard.vue`](../web-fit-vue/src/components/workout/RankingBoard.vue) 已有坚持榜+进步榜但展示简陋
> - ⚠️ **致命缺陷**：[`GymWorkoutRecord`](../java-fit-server/src/main/java/com/fit/entity/GymWorkoutRecord.java) 表只有 `actionId / muscleGroup / startTime / endTime / exhaustionScore / status`，**完全没有重量/次数/组数字段**
> - 现有排行榜数据源是 `training_session` + `training_session_detail`（旧训练计划系统），与新的 `gym_workout_record` 系统是割裂的
>
> **核心前提**：健身榜单的「容量榜」「1RM巅峰榜」「进步榜」都需要重量×次数×组数的数据，而这些数据在当前健身打卡流程中**从未被采集**。因此必须先改造训练计时流程，让用户在训练中/训练后填写重量和次数。

---

## 改动范围总览

| 层 | 文件 | 改动类型 | 所属阶段 |
|----|------|----------|----------|
| **数据库** | **新建** `V29__extend_gym_workout_record.sql` | Flyway 迁移 | Phase 0 |
| **实体** | `GymWorkoutRecord.java` | 添加字段 | Phase 0 |
| **VO** | `WeeklyWorkoutVO.java` | 添加字段 | Phase 0 |
| **后端Service** | `GymWorkoutRecordService.java` / `Impl` | endWorkout 签名扩展 + 数据写入 | Phase 0 |
| **后端Controller** | `GymWorkoutRecordController.java` | endWorkout 参数扩展 | Phase 0 |
| **前端API** | `gymWorkout.ts` | `endWorkout` 参数扩展 | Phase 0 |
| **前端组件** | `TrainingTimerDialog.vue` | **重写**：加入重量/次数/组数输入区 | Phase 0 |
| **后端VO** | **新建** `RankingItemVO.java` | 榜单统一 VO | Phase 1 |
| **后端Service** | `TrainingStatsService.java` / `Impl` | 榜单查询切换到 `gym_workout_record` 数据源 | Phase 1 |
| **后端Controller** | `TrainingStatsController.java` | 新增4个榜单端点 | Phase 1 |
| **前端API** | `trainingStats.ts` | 新增 `RankingItemVO` 类型 + API 函数 | Phase 1 |
| **前端组件** | `RankingBoard.vue` | **重写**：卡片化 | Phase 1 |
| **前端页面** | `HealthView.vue` | **重写**：榜单页 | Phase 1 |
| **菜单** | `SideMenu.vue` | 文案修改 | Phase 1 |
| **首页** | `HomeView.vue` | 文案修改 | Phase 1 |
| **路由** | `router/index.ts` | 不变 | — |

---

## Phase 0：训练数据记录（前提）

### 背景

当前训练流程：
```
选择肌肉 → 选择动作 → 开始计时 → ...训练中... → 结束计时 → 评价力竭度 → 保存
                                                                    ↓
                                                        只存了：startTime, endTime, exhaustionScore
                                                        缺失：❌ weight(重量) ❌ reps(次数) ❌ sets(组数)
```

改造后的流程：
```
选择肌肉 → 选择动作 → 开始计时 → ...训练中... → 结束计时 → 填写本次训练数据 → 评价力竭度 → 保存
                                          ↑ 实时可编辑              ↑ 结束前确认              ↓
                                    重量/次数/组数框                                  存：weight, reps, setCount
```

### 0.1 数据库变更（Flyway V29）

```sql
-- V29__extend_gym_workout_record.sql
ALTER TABLE gym_workout_record
    ADD COLUMN weight      DECIMAL(6,2)  DEFAULT NULL COMMENT '本次动作使用重量(kg)，NULL表示无负重（自重/有氧/拉伸等）',
    ADD COLUMN reps        INT           DEFAULT NULL COMMENT '本次动作训练次数，NULL表示未填写或不适用',
    ADD COLUMN set_count   INT           DEFAULT NULL COMMENT '本次训练组数，NULL表示未填写或不适用',
    ADD COLUMN rm_estimate DECIMAL(7,2)  DEFAULT NULL COMMENT '自动计算的1RM估值(kg)，仅当weight+reps均非NULL时计算',
    ADD COLUMN is_pr       TINYINT       DEFAULT 0 COMMENT '是否破个人纪录 0=否 1=是';
```

> ⚠️ **所有新字段 `DEFAULT NULL`**：兼容自重/有氧/拉伸等无需负重的动作，也允许用户跳过不填。

### 0.2 动作类型与数据填写策略

| `gym_action.actionType` | 典型动作 | 需要填的字段 | 是否计入容量榜/1RM榜 |
|--------------------------|----------|-------------|---------------------|
| `COMPOUND` | 杠铃卧推、深蹲、硬拉 | weight + reps + setCount | ✅ 是 |
| `ISOLATION` | 哑铃飞鸟、腿屈伸 | weight + reps + setCount | ✅ 是 |
| `COMPOUND` 自重 | 引体向上、双杠臂屈伸 | reps + setCount（weight=0 或留空） | ❌ 容量榜不计，坚持榜计入 |
| `CARDIO` | 跑步机、椭圆机、划船机 | 全部留空（仅计时） | ❌ 仅坚持榜 |
| `STRETCH` | TRX拉伸 | 全部留空 | ❌ 仅坚持榜 |
| `MOBILITY` | 泡沫轴滚揉 | 全部留空 | ❌ 仅坚持榜 |

### 0.3 `GymWorkoutRecord.java` 实体扩展

新增字段（全部可为 null）：
```java
/** 本次动作使用重量(kg)，NULL表示无负重或不适用 */
private BigDecimal weight;

/** 本次动作训练次数，NULL表示未填写或不适用 */
private Integer reps;

/** 本次训练组数，NULL表示未填写或不适用 */
private Integer setCount;

/** 自动计算的1RM估值(kg)，仅当weight+reps均非NULL时计算 */
private BigDecimal rmEstimate;

/** 是否破个人纪录：0=否, 1=是 */
private Integer isPr;
```

### 0.4 后端接口改动

**`GymWorkoutRecordService.endWorkout`** 签名扩展：
```java
// 旧
void endWorkout(String recordId, BigDecimal exhaustionScore);

// 新（所有训练数据字段均可为 null）
void endWorkout(String recordId, BigDecimal weight, Integer reps, Integer setCount, BigDecimal exhaustionScore);
```

**`GymWorkoutRecordController`** — `PUT /api/gym-workout/{id}/end` 请求体扩展：
```json
// 旧 body: { "exhaustionScore": 0.85 }
// 新 body（weight/reps/setCount 全部可选）:
//   负重动作：{ "weight": 60.0, "reps": 10, "setCount": 4, "exhaustionScore": 0.85 }
//   自重动作：{ "reps": 12, "setCount": 5, "exhaustionScore": 0.80 }
//   有氧动作：{ "exhaustionScore": 0.70 }
```

**后端实现逻辑**（`GymWorkoutRecordServiceImpl.endWorkout`）：
```java
public void endWorkout(String recordId, BigDecimal weight, Integer reps, Integer setCount, BigDecimal exhaustionScore) {
    GymWorkoutRecord record = getRecordOrThrow(recordId);
    if (record.getStatus() != 0) throw new IllegalStateException("该训练记录已结束");

    // 1. 写入训练数据（允许全部为 null）
    record.setWeight(weight);
    record.setReps(reps);
    record.setSetCount(setCount);

    // 2. 仅当 weight 和 reps 均非 null 时才计算 1RM 和 PR
    if (weight != null && reps != null && weight.compareTo(BigDecimal.ZERO) > 0 && reps > 0) {
        BigDecimal rm = weight.multiply(BigDecimal.ONE.add(
            BigDecimal.valueOf(reps).divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP)))
            .setScale(2, RoundingMode.HALF_UP);
        record.setRmEstimate(rm);

        // 查该用户该动作历史最佳 1RM，判断是否 PR
        BigDecimal bestRm = findBestRmEstimate(record.getUserId(), record.getActionId());
        record.setIsPr(bestRm != null && rm.compareTo(bestRm) > 0 ? 1 : 0);
    }
    // 如果 weight/reps 任一为 null，rmEstimate 保持 null，isPr 保持 0

    // 3. 写入 endTime + exhaustionScore + status=1
    record.setEndTime(LocalDateTime.now());
    record.setExhaustionScore(exhaustionScore);
    record.setStatus(1);
    recordMapper.updateById(record);
}
```

### 0.5 前端改造：`TrainingTimerDialog.vue` 重写

在训练计时弹窗中增加**按动作类型自适应的数据输入区**。前端通过 `listByActionId` 能拿到动作详情（含 `actionType`），根据类型决定显示哪些输入框。

**三种输入模式**：

```
模式 A：负重动作（COMPOUND / ISOLATION，非自重类）
┌──────────────────────────────────────────────┐
│  训练计时 — 杠铃卧推                         │
│             01:23:45                         │
│  ┌──────────────────────────────────────┐    │
│  │ 📊 训练数据                           │    │
│  │  重量 (kg)  [  60.0  ]               │    │
│  │  次数       [  10    ]               │    │
│  │  组数       [  4     ]               │    │
│  │  ──────────────────────────────      │    │
│  │  估计 1RM：80.0 kg  总容量：2400 kg   │    │
│  └──────────────────────────────────────┘    │
└──────────────────────────────────────────────┘

模式 B：自重动作（引体向上、双杠臂屈伸、平板支撑等）
┌──────────────────────────────────────────────┐
│  训练计时 — 宽握引体向上                      │
│             00:15:30                         │
│  ┌──────────────────────────────────────┐    │
│  │ 📊 训练数据（自重动作）                │    │
│  │  次数  [  8  ]  组数  [  4  ]       │    │
│  │  💡 自重动作，无需填写重量             │    │
│  └──────────────────────────────────────┘    │
└──────────────────────────────────────────────┘

模式 C：有氧/拉伸/活动度（CARDIO / STRETCH / MOBILITY）
┌──────────────────────────────────────────────┐
│  训练计时 — 跑步机慢跑                        │
│             00:30:00                         │
│  ┌──────────────────────────────────────┐    │
│  │ 💡 有氧训练，仅记录训练时长和力竭度    │    │
│  │    无需额外填写重量/次数数据           │    │
│  └──────────────────────────────────────┘    │
└──────────────────────────────────────────────┘
```

**判定逻辑**（前端，基于 `actionType` + 动作名特征）：
```ts
type InputMode = 'weighted' | 'bodyweight' | 'cardio'

function getInputMode(actionType: string, actionName: string): InputMode {
  if (['CARDIO', 'STRETCH', 'MOBILITY'].includes(actionType)) return 'cardio'
  // 自重判定：动作名含"引体向上""双杠""平板支撑""悬垂举腿"等关键词
  const bodyweightKeywords = ['引体向上', '双杠臂屈伸', '平板支撑', '悬垂举腿', '仰卧抬腿', '基础卷腹']
  if (bodyweightKeywords.some(k => actionName.includes(k))) return 'bodyweight'
  return 'weighted'
}
```

**Props 扩展**：
```ts
const props = defineProps<{
  visible: boolean
  actionId: string
  actionName: string
  actionType?: string           // 新增：COMPOUND/ISOLATION/CARDIO/STRETCH/MOBILITY
  defaultSets?: { min: number; max: number }
  defaultReps?: { min: number; max: number }
}>()
```

**Emits 扩展**：
```ts
const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'end', payload: {
    elapsedSeconds: number
    weight?: number      // 可选
    reps?: number        // 可选
    setCount?: number    // 可选
  }): void
  (e: 'cancel'): void
}>()
```

### 0.6 前端页面适配：`GymWorkoutView.vue`

需要将 `actionType` 传递给 `TrainingTimerDialog`。在 `handleActionStart` 中获取动作详情：

```ts
async function handleActionStart(actionId: string, actionName: string, actionType?: string): Promise<void> {
  try {
    const res = await startWorkout(actionId)
    currentRecordId.value = res.data
    currentActionId.value = actionId
    currentActionName.value = actionName
    currentActionType.value = actionType || ''
    timerVisible.value = true
  } catch {
    ElMessage.error('开始训练失败')
  }
}
```

`handleTimerEnd` 适配可选字段：
```ts
function handleTimerEnd(payload: {
  elapsedSeconds: number; weight?: number; reps?: number; setCount?: number
}): void {
  pendingWorkoutData.value = {
    weight: payload.weight ?? null,
    reps: payload.reps ?? null,
    setCount: payload.setCount ?? null,
  }
  exhaustionVisible.value = true
}
```

### 0.7 `WeeklyWorkoutVO` 扩展

```java
private BigDecimal weight;     // 可为 null
private Integer reps;          // 可为 null
private Integer setCount;      // 可为 null
private BigDecimal rmEstimate; // 可为 null
private Boolean isPr;
```

### 0.8 榜单计算中的容错策略

**所有榜单查询必须在 WHERE 条件中排除数据不完整的记录**：

| 榜单 | 数据要求 | WHERE 条件 |
|------|---------|-----------|
| 🏅 坚持榜 | 仅需 startTime（日期去重） | `status IN (1,2)` — **无额外过滤**，所有动作类型都计入 |
| 📊 容量榜 | 需要 weight × reps × setCount | `weight IS NOT NULL AND reps IS NOT NULL AND set_count IS NOT NULL` |
| 💪 1RM巅峰榜 | 需要 rmEstimate | `rm_estimate IS NOT NULL` + 三大项动作筛选 |
| 📈 进步榜 | 需要前后周期都有 rmEstimate | `rm_estimate IS NOT NULL`（前后各自有数据才纳入计算） |

**容量榜示例 SQL**：
```sql
SELECT user_id, SUM(weight * reps * set_count) as total_vol
FROM gym_workout_record
WHERE status IN (1,2)
  AND start_time >= ?
  AND weight IS NOT NULL
  AND reps IS NOT NULL
  AND set_count IS NOT NULL
  AND weight > 0 AND reps > 0 AND set_count > 0
GROUP BY user_id
ORDER BY total_vol DESC
```

> ⚠️ 关键：没有填重量/次数的记录自动被榜单忽略，不影响坚持榜（坚持榜只看打卡天数）。

---

## Phase 1：榜单页面改造

> Phase 1 依赖 Phase 0 完成（`gym_workout_record` 表已有 weight/reps/setCount 数据）。

### 1.1 菜单与入口改造

**SideMenu.vue**：
```diff
- import { HomeFilled, TrendCharts, Tools, ForkSpoon, Cpu, Calendar, Timer } from '@element-plus/icons-vue'
+ import { HomeFilled, Trophy, Tools, ForkSpoon, Cpu, Calendar, Timer } from '@element-plus/icons-vue'

- { path: '/health', title: '健康数据', icon: TrendCharts },
+ { path: '/health', title: '健身榜单', icon: Trophy },
```

**HomeView.vue**：
```diff
- { path: '/health', title: '健康数据', desc: '体重、BMI 等体征指标记录', icon: '📊', color: '#409eff' },
+ { path: '/health', title: '健身榜单', desc: '坚持榜、容量榜、1RM巅峰榜、进步榜', icon: '🏆', color: '#e6a23c' },
```

### 1.2 榜单维度设计

| 榜单 | 分类 | 说明 | 数据来源（Phase 0 改造后） |
|------|------|------|-----------|
| 🏅 **坚持榜** | 勤奋榜 | 周期内累计打卡天数排名 | `gym_workout_record` 按 `DATE(start_time)` 去重计数 |
| 📊 **容量榜** | 勤奋榜 | 周期内训练总容量（`weight × reps × setCount`）排名 | `gym_workout_record` SUM(weight*reps*setCount) |
| 💪 **1RM巅峰榜** | 硬核榜 | 三大项（深蹲+卧推+硬拉）1RM总和排名 | `gym_workout_record` + `gym_action` JOIN，取各动作最大 rmEstimate |
| 📈 **进步榜** | 硬核榜 | 相比上个周期 1RM 增长百分比排名 | `gym_workout_record` 前后周期 rmEstimate 对比 |

### 1.3 后端：新建 `RankingItemVO.java`

```java
// java-fit-server/src/main/java/com/fit/vo/RankingItemVO.java
package com.fit.vo;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class RankingItemVO {
    private int rank;
    private String userId;
    private String empName;
    private String empNo;
    private String avatarUrl;
    /** 核心数据值（天数/容量kg/1RMkg/增长率%） */
    private BigDecimal value;
    /** 辅助数据1 */
    private BigDecimal auxiliaryValue;
    /** 辅助数据2 */
    private BigDecimal auxiliaryValue2;
    /** 趋势 */
    private String trend;
}
```

### 1.4 后端：榜单查询实现

**关键变更**：榜单数据源从 `training_session` + `training_session_detail` **切换到** `gym_workout_record`（Phase 0 已扩展 weight/reps/setCount/rmEstimate 字段）。

#### 1.4.1 容错策略（三不原则）

> ⚠️ **与 Phase 0.8 一致**：所有榜单查询必须正确处理用户未填写训练数据的场景。

| 榜单 | 数据要求 | 容错策略 |
|------|---------|----------|
| 🏅 坚持榜 | 仅需 `startTime`（日期去重） | `status IN (1,2)` — **无额外过滤**，有氧/拉伸/自重/负重全部计入 |
| 📊 容量榜 | 需要 `weight × reps × set_count` | `weight IS NOT NULL AND reps IS NOT NULL AND set_count IS NOT NULL AND weight > 0 AND reps > 0 AND set_count > 0` — NULL 或零值记录自动排除 |
| 💪 1RM巅峰榜 | 需要 `rm_estimate` | `rm_estimate IS NOT NULL` + JOIN `gym_action` 筛选三大项（`actionType = 'COMPOUND'`） |
| 📈 进步榜 | 前后周期均需 `rm_estimate` | 前后两段分别对 `rm_estimate IS NOT NULL` 的记录聚合，任一周期无数据则不纳入排名 |

#### 1.4.2 端点与查询逻辑

| 方法 | 端点 | 查询逻辑 |
|------|------|----------|
| `getConsistencyRankingV2(int days)` | `GET /ranking/consistency-v2` | `SELECT user_id, COUNT(DISTINCT DATE(start_time)) as days FROM gym_workout_record WHERE start_time >= ? AND status IN (1,2) GROUP BY user_id ORDER BY days DESC` → JOIN user 表取头像/姓名。**不做任何 weight/reps 过滤，有氧/自重/拉伸动作均计入打卡天数** |
| `getVolumeRanking(int days)` | `GET /ranking/volume` | `SELECT user_id, SUM(weight * reps * set_count) as total_vol FROM gym_workout_record WHERE start_time >= ? AND status IN (1,2) AND weight IS NOT NULL AND reps IS NOT NULL AND set_count IS NOT NULL AND weight > 0 AND reps > 0 AND set_count > 0 GROUP BY user_id ORDER BY total_vol DESC` → JOIN user。**只汇总填写了完整重量×次数×组数的记录** |
| `getPeak1RMRanking(int days)` | `GET /ranking/peak-1rm` | 1. 从 `gym_action` 筛选三大项（`actionType = 'COMPOUND'` + 名称含"深蹲/卧推/硬拉"） 2. JOIN `gym_workout_record` WHERE `rm_estimate IS NOT NULL` 3. 每用户每项取 `MAX(rm_estimate)` 4. 三项求和排名 5. JOIN user。**rm_estimate IS NOT NULL 确保只统计有重量×次数的记录** |
| `getProgressRankingV2(int days)` | `GET /ranking/progress-v2` | 分前后两段周期（`[now-2*days, now-days)` vs `[now-days, now]`），各取 `rm_estimate IS NOT NULL` 的记录按用户聚合，计算增长率 `(本期总和 - 上期总和) / 上期总和 × 100%` 排名。**前后任一周期无有效 rm_estimate 数据的用户自动排除** |

**Mapper 依赖**：
- 需要在 `GymWorkoutRecordMapper` 中添加自定义查询方法或使用 MyBatis-Plus LambdaQueryWrapper

### 1.5 前端：页面布局 — `HealthView.vue` 重写

```
┌──────────────────────────────────────────────────────────┐
│  🏆 健身榜单                                              │
│  [ 本周 ] [ 本月 ] [ 本季度 ]     ← 时间维度切换 el-radio │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  ┌─────────────────────┐  ┌─────────────────────┐       │
│  │ 🏅 坚持榜           │  │ 📊 容量榜           │       │
│  │ ─────────────────── │  │ ─────────────────── │       │
│  │                     │  │                     │       │
│  │ ╔══ 🥇 #1 ══════╗  │  │ ╔══ 🥇 #1 ══════╗  │       │
│  │ ║ 张三  1888888  ║  │  │ ║ 李四  8.5吨   ║  │       │
│  │ ║ 训练 18 天     ║  │  │ ║ 总容量排名第一 ║  │       │
│  │ ╚════════════════╝  │  │ ╚════════════════╝  │       │
│  │ ┌── 🥈 #2 ──────┐  │  │ ┌── 🥈 #2 ──────┐  │       │
│  │ │ 李四  15天     │  │  │ │ 王五  6.2吨    │  │       │
│  │ └───────────────┘  │  │ └───────────────┘  │       │
│  │ ┌── 🥉 #3 ──────┐  │  │ ┌── 🥉 #3 ──────┐  │       │
│  │ │ 王五  12天     │  │  │ │ 张三  5.1吨    │  │       │
│  │ └───────────────┘  │  │ └───────────────┘  │       │
│  │ ... 4-10名列表 ... │  │ ... 4-10名列表 ... │       │
│  │ ────────────────── │  │ ────────────────── │       │
│  │ 📌 你的排名 #5 14天 │  │ 📌 你的排名 #3 5.1吨│       │
│  └─────────────────────┘  └─────────────────────┘       │
│                                                          │
│  ┌─────────────────────┐  ┌─────────────────────┐       │
│  │ 💪 1RM巅峰榜        │  │ 📈 进步榜           │       │
│  │ （三大项总和）       │  │ （环比上期增长）     │       │
│  │ ╔🥇 张三 320kg╗    │  │ ╔🥇 王五 +25% ╗    │       │
│  │ ...                 │  │ ...                 │       │
│  └─────────────────────┘  └─────────────────────┘       │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

**页面级状态**：
```ts
const period = ref<'week' | 'month' | 'quarter'>('week')
const periodDays = computed(() => period.value === 'week' ? 7 : period.value === 'month' ? 30 : 90)

// 四个榜单数据
const consistencyData = ref<RankingItemVO[]>([])
const volumeData = ref<RankingItemVO[]>([])
const peak1rmData = ref<RankingItemVO[]>([])
const progressData = ref<RankingItemVO[]>([])

// loading
const loadingConsistency = ref(false)
const loadingVolume = ref(false)
const loadingPeak1rm = ref(false)
const loadingProgress = ref(false)
```

### 1.6 前端：RankingBoard.vue 重写

**设计规范**：
- 🥇 第1名：金色渐变卡片 `background: linear-gradient(135deg, #fff9e6, #fff3cc); border: 2px solid #ffd700`
- 🥈 第2名：银色 `background: #f8f8f8; border: 1px solid #c0c0c0`
- 🥉 第3名：铜色 `background: #fef5ee; border: 1px solid #cd7f32`
- 4~10名：白色卡片 `shadow: hover`
- **底部固定栏**（PRD 要求）：`sticky bottom:0`，蓝色背景高亮当前用户排名

**Props**：
```ts
defineProps<{
  title: string
  icon: string
  loading: boolean
  data: RankingItemVO[]
  valueLabel: string              // "天" | "吨" | "kg" | "%"
  valueFormatter?: (v: number) => string
  currentUserEmpNo: string        // 用于识别当前用户在列表中的位置
}>()
```

### 1.7 前端 API：`trainingStats.ts` 扩展

```ts
export interface RankingItemVO {
  rank: number
  userId: string
  empName: string
  empNo: string
  avatarUrl: string
  value: number
  auxiliaryValue?: number
  auxiliaryValue2?: number
  trend?: string
}

export function getConsistencyRankingV2(days: number): Promise<ApiResult<RankingItemVO[]>>
export function getVolumeRanking(days: number): Promise<ApiResult<RankingItemVO[]>>
export function getPeak1RMRanking(days: number): Promise<ApiResult<RankingItemVO[]>>
export function getProgressRankingV2(days: number): Promise<ApiResult<RankingItemVO[]>>
```

> 旧版 `getConsistencyRanking` / `getProgressRanking` 保留不动。

---

## 数据流总览

```
用户训练流程（Phase 0）
═══════════════════════════════════════════════════════
GymWorkoutView → ActionSelectDialog → TrainingTimerDialog
                                          │
                                  用户填写重量/次数/组数
                                          │
                                   结束训练 → emit('end', {weight, reps, setCount})
                                          │
                                 ExhaustionDialog → endWorkout(weight, reps, setCount, score)
                                          │
                              GymWorkoutRecord 写入：
                              { weight, reps, setCount, rmEstimate, isPr, exhaustionScore }

榜单查询流程（Phase 1）
═══════════════════════════════════════════════════════
HealthView.vue
  │  onMounted + watch(period) → periodDays 计算
  │
  ├── GET /api/training-stats/ranking/consistency-v2?days=N
  │     → gym_workout_record COUNT(DISTINCT DATE(start_time)) GROUP BY user_id → JOIN user
  │
  ├── GET /api/training-stats/ranking/volume?days=N
  │     → gym_workout_record SUM(weight * reps * set_count) GROUP BY user_id → JOIN user
  │
  ├── GET /api/training-stats/ranking/peak-1rm?days=N
  │     → gym_workout_record JOIN gym_action 筛选三大项 MAX(rm_estimate) → JOIN user
  │
  └── GET /api/training-stats/ranking/progress-v2?days=N
        → gym_workout_record 前后周期 rmEstimate 对比 → JOIN user
```

---

## 实施步骤

| 步骤 | 阶段 | 内容 | 预估 | 依赖 |
|------|------|------|------|------|
| **Step 0** | Phase 0 | 数据库：Flyway V29 — `gym_workout_record` 加 weight/reps/set_count/rm_estimate/is_pr | 小（15min） | — |
| **Step 1** | Phase 0 | 后端：`GymWorkoutRecord.java` 实体加字段 | 小（5min） | Step 0 |
| **Step 2** | Phase 0 | 后端：`GymWorkoutRecordService.endWorkout` 签名扩展 → `Impl` 实现 weight/reps/1RM/PR 写入逻辑 | 中（45min） | Step 1 |
| **Step 3** | Phase 0 | 后端：`GymWorkoutRecordController` 接口参数扩展 | 小（10min） | Step 2 |
| **Step 4** | Phase 0 | 前端：`gymWorkout.ts` — `endWorkout` 参数扩展 | 小（5min） | Step 3 |
| **Step 5** | Phase 0 | 前端：**重写** `TrainingTimerDialog.vue` — 加重量/次数/组数输入区 + 实时 1RM/容量计算 | 中（1h） | Step 4 |
| **Step 6** | Phase 0 | 前端：`GymWorkoutView.vue` — 适配新的 `end` 回调签名 + `endWorkout` 调用 | 小（20min） | Step 5 |
| **Step 7** | Phase 0 | `WeeklyWorkoutVO.java` 扩展字段 | 小（5min） | Step 2 |
| — | — | **Phase 0 完成，可独立测试** | — | — |
| **Step 8** | Phase 1 | 后端：新建 `RankingItemVO.java` | 小（10min） | — |
| **Step 9** | Phase 1 | 后端：`TrainingStatsService` 新增4个方法签名 | 小（5min） | Step 8 |
| **Step 10** | Phase 1 | 后端：`TrainingStatsServiceImpl` 实现4个榜单查询（基于 `gym_workout_record` 新字段） | 中（1.5h） | Step 9 |
| **Step 11** | Phase 1 | 后端：`TrainingStatsController` 新增4个端点 | 小（10min） | Step 10 |
| **Step 12** | Phase 1 | 前端：`trainingStats.ts` 新增 `RankingItemVO` 类型 + 4个 API 函数 | 小（15min） | Step 11 |
| **Step 13** | Phase 1 | 前端：**重写** `RankingBoard.vue` — 卡片化（金银铜 + 底部固定栏） | 中（1h） | Step 12 |
| **Step 14** | Phase 1 | 前端：**重写** `HealthView.vue` — 四榜网格 + 时间切换 | 中（45min） | Step 13 |
| **Step 15** | Phase 1 | 前端：修改 `SideMenu.vue` + `HomeView.vue` 文案/图标 | 小（10min） | — |
| **Step 16** | — | 联调 + UI 微调 | 小（30min） | Step 14 |

**建议执行顺序**：Step 0 → 1 → 2 → 3 → 4 → 5 → 6 → 7 →（Phase 0 测试）→ 8 → 9 → 10 → 11 → 12 → 13 → 14 → 15 → 16

---

## 关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 榜单数据源 | **切换到 `gym_workout_record`**（原为 `training_session_detail`） | Phase 0 已补齐 weight/reps 字段，统一数据源避免割裂 |
| 重量/次数输入位置 | **计时弹窗内实时可编辑** | 训练中途可以修改，结束前确认，符合"中途填或者改"需求 |
| 1RM 计算公式 | **Epley: W × (1 + R/30)** | PRD 推荐，国际通用标准 |
| 组数记录 | **单选一组的总次数**（非逐组拆分） | 简化一期实现，PRD 的"逐组记录"留二期 |
| 旧版 RankingBoard 小程序端 | **保留不动** | 等小程序端有需求时再跟进 |
| 容量榜单位 | **前端换算**（后端返 kg，前端 /1000 显示吨） | 保持后端精度 |
| 第1名样式 | **金色渐变卡片** | PRD 要求"金色光芒边框" |
| 当前用户排名 | **底部 sticky 固定栏** | PRD 要求"永远固定在底部显著位置" |

---

## 不变动的范围

- `GymWorkoutView.vue` 肌群卡片布局 — 不变
- `MuscleDashboard.vue` — 不变
- `ActionSelectDialog.vue` — 不变
- `ExhaustionDialog.vue` — 不变（仅力竭度，已足够）
- 小程序端 `mp-fit-ts` — 本次仅改造 Web 端
- `training_session` / `training_session_detail` 表 — 保留不动（旧系统数据不丢）
- `GymWorkoutRecordService` 其他方法（startWorkout, correctTimeout, makeupWorkout, getDashboard）— 逻辑不变
