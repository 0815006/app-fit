# 健身打卡页面改造计划（Web 端）

> 目标：将健身打卡首页从「双层展开式卡片」改造为「**大肌群卡片 + 二级肌肉平铺列表**」，入口逻辑采用**方案 B（整个肌群动作平铺，特定二级置顶高亮）**。

---

## 一、改造总览

### 1.1 改动范围

| 层 | 文件 | 改动类型 |
|----|------|----------|
| 页面 | [`GymWorkoutView.vue`](../web-fit-vue/src/views/GymWorkoutView.vue) | 重构：简化事件处理逻辑 |
| 组件 | [`MuscleDashboard.vue`](../web-fit-vue/src/components/workout/MuscleDashboard.vue) | **重写**：卡片布局彻底改造 |
| 组件 | [`ActionSelectDialog.vue`](../web-fit-vue/src/components/workout/ActionSelectDialog.vue) | **重写**：支持方案B置顶高亮 |
| API | [`gymWorkout.ts`](../web-fit-vue/src/api/gymWorkout.ts) | 追加类型定义 |
| 后端 | `GymWorkoutRecordServiceImpl.java` | 看板接口追加二级肌肉统计 |
| 后端 | `GymActionController.java` | 新增按肌群+动作肌肉关系查询接口 |

### 1.2 不改动的范围

- `WeeklySummary.vue` — 本周训练概览（七日网格）保持不变
- `TrainingTimerDialog.vue` — 计时弹窗不变
- `ExhaustionDialog.vue` — 力竭度弹窗不变
- `TimeoutCorrectDialog.vue` / `MakeupDialog.vue` — 不变
- 数据库 Flyway 脚本 — 无需新增迁移（现有表结构已够用）

---

## 二、首页卡片改造：`MuscleDashboard.vue` 重写

### 2.1 现状 vs 目标

```
【现状】                              【目标】
┌──────────────────────┐          ┌──────────────────────┐
│ ▶ 💪 胸部  🔥×2       │          │ 胸部          🟢 可练  │  ← 卡片头部
│       🟢 已恢复        │          │                      │
│ [ 从 胸部 入口 ]       │          │ 胸大肌           🔥  │  ← 二级肌肉行
│ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ │          │ 胸上束               │  ← 二级肌肉行
│   • 胸大肌  进入训练   │          │ 胸下束           🔥  │  ← 二级肌肉行
│   • 胸上束  进入训练   │          │                      │
└──────────────────────┘          └──────────────────────┘
```

### 2.2 新卡片布局规范

每个大肌群一张卡片，内部结构固定为：

```
┌─────────────────────────────────────────┐
│  胸部                        🟢 可练     │  ← header: 左侧加粗肌群名，右侧恢复标签
├─────────────────────────────────────────┤
│  胸大肌                          🔥     │  ← 二级肌肉行（始终展开，不可折叠）
│  胸上束                                │     每行左侧肌肉名，右侧本周痕迹
│  胸下束                          🔥     │
└─────────────────────────────────────────┘
```

**卡片头部（一级主信息）**：
- 左上角：**大字加粗**肌群中文名（如「胸部」），字体 16px, font-weight 700
- 右上角：动态恢复状态标签
  - 🟢 可练 → `el-tag type="success"` 小绿点
  - ⏳ 18h → `el-tag type="warning"` 带剩余时间
- 背景色/边框：
  - `READY`：正常白色背景，`box-shadow: hover`
  - `RECOVERING`：整个卡片 `opacity: 0.6`，背景 `#f5f5f5`

**卡片下半部分（二级肌肉列表）**：
- 垂直列表，一个肌肉一行，**始终展开**（去掉展开/折叠交互）
- 每行：
  - 左侧：肌肉中文名（如「胸大肌」），14px, color #303133
  - 右侧：本周打卡痕迹 `🔥`（仅当该肌肉本周有训练记录时显示）
- 行交互：`hover` 时背景变为 `rgba(64, 158, 255, 0.06)`，cursor pointer
- **点击整行** → 触发 `selectMuscle(muscle)` 事件，携带该肌肉的 `muscleCode`

### 2.3 移除的内容

- ❌ 展开/折叠箭头（`▶`/`▼`）
- ❌ 「从 XX 入口」快捷按钮
- ❌ 大肌群级别的 `emit('select', group)` 入口
- ❌ 空状态提示（`el-empty`，因为肌群卡片始终存在）

### 2.4 Props / Emits 调整

```ts
// Props（简化：不再需要 expand 相关状态）
defineProps<{
  muscleGroups: MuscleGroupStatusVO[]    // 大肌群状态（含整体恢复）
  muscles: GymMuscle[]                    // 所有二级肌肉
  /** 每个二级肌肉本周是否有训练记录，key = muscleCode */
  muscleWeeklyFlags: Record<string, boolean>
  loading: boolean
}>()

// Emits（简化：只保留二级肌肉点击）
defineEmits<{
  (e: 'selectMuscle', muscle: GymMuscle): void
}>()
```

### 2.5 新增数据：二级肌肉本周训练痕迹

当前后端 `getDashboard` 只按 `muscle_group` 分组统计本周次数。要展示二级肌肉的 🔥，需要额外数据。

**方案 A（推荐，后端扩展）**：在 `MuscleGroupStatusVO` 中追加字段：

```java
// MuscleGroupStatusVO 新增字段
private List<SubMuscleStatus> subMuscles;

// 内嵌类
public static class SubMuscleStatus {
    private String muscleCode;    // 如 CHEST_MAJOR
    private String muscleName;    // 如 胸大肌
    private boolean trainedThisWeek;  // 本周是否有训练记录
}
```

后端查询逻辑：对每个 `muscleGroup`，查出其下所有 `gym_muscle`，再逐个子查 `gym_workout_record` 本周是否有 `action_id` 通过 `gym_action_muscle_rel` 关联到该 `muscle_id`。

**方案 B（前端兜底，无需后端改动）**：前端利用已有的 `getWeeklySummary` 返回的 `WeeklyWorkoutVO[]`（含 `actionName` + `muscleGroup`），配合加载 `listByActionId` 获取每个动作关联的 `muscleId`，在本地计算每个二级肌肉是否被训练过。

**本次改造采用方案 A**（一次 API 返回，前端无额外计算）。

---

## 三、入口逻辑改造：方案 B「整群动作平铺 + 特定二级置顶高亮」

### 3.1 交互流程

```
用户点击「胸上束」行
        │
        ▼
打开 ActionSelectDialog
    ├── 传入 muscleGroup = "CHEST"
    └── 传入 highlightMuscleCode = "CHEST_MAJOR"  ← 本次点击的二级肌肉编码
        │
        ▼
弹窗内部：
    1. 调用 GET /api/gym-action/by-muscle-group/CHEST  获取全群动作
    2. 调用 GET /api/gym-action-muscle-rel/by-muscle-group/CHEST  获取所有动作-肌肉关联
    3. 前端排序：
       - 与 highlightMuscleCode 匹配的动作 → 排在最前面，加高亮框
       - 其他动作 → 排在后面，正常样式
    4. 渲染列表（见 3.2）
```

### 3.2 ActionSelectDialog 新布局

```
┌──────────────────────────────────────────────┐
│  选择训练动作 — 胸上束                     ✕  │  ← 标题带二级肌肉名
├──────────────────────────────────────────────┤
│  [ 胸上束 ] [ 胸中束 ] [ 胸下束 ]             │  ← 可选：横向二级肌肉切换 Tab
├──────────────────────────────────────────────┤
│  ╔══════════════════════════════════════╗    │
│  ║ 🌟 推荐 — 胸上束                     ║    │  ← 置顶高亮区域
│  ╠══════════════════════════════════════╣    │
│  ║ 杠铃上斜卧推        [开始训练]        ║    │  ← 匹配的动作，金色边框
│  ║ 📋 4~6组, 8~12次, 休息90s           ║    │
│  ║ 哑铃上斜飞鸟        [开始训练]        ║    │
│  ╚══════════════════════════════════════╝    │
│                                              │
│  ┌──────────────────────────────────────┐    │
│  │ 平板杠铃卧推        [开始训练]        │    │  ← 常规动作，普通卡片
│  │ 📋 4~6组, 8~12次, 休息90s           │    │
│  ├──────────────────────────────────────┤    │
│  │ 哑铃飞鸟            [开始训练]        │    │
│  └──────────────────────────────────────┘    │
└──────────────────────────────────────────────┘
```

### 3.3 高亮区域设计规范

- **推荐区域**：使用 `el-card` + 自定义 class `highlight-section`
  - 左侧金色竖条（`border-left: 4px solid #e6a23c`）
  - 背景 `linear-gradient(to right, rgba(230,162,60,0.08), transparent)`
  - 标题栏：「🌟 推荐 — 胸上束」，字体加粗
- **常规区域**：普通 `el-card`，`shadow="hover"`
- **区域间隔**：推荐区与常规区之间加分割文本「—— 其他胸部动作 ——」或直接用间距分隔

### 3.4 可选增强：横向二级肌肉切换 Tab

在弹窗顶部添加 `el-tabs`（`type="card"` 小尺寸），列出该大肌群下所有二级肌肉。切换 Tab 时改变 `highlightMuscleCode`，对应动作列表自动重新排序。

**一期是否做**：标记为「可选」，先做核心排序高亮，Tab 切换作为用户体验优化迭代。

### 3.5 Props / Emits 调整

```ts
// ActionSelectDialog Props
defineProps<{
  visible: boolean
  muscleGroup: string              // 大肌群编码，如 "CHEST"
  /** 用户点击的具体肌肉编码，用于高亮置顶 */
  highlightMuscleCode?: string     // 如 "CHEST_MAJOR"
  /** 用户点击的具体肌肉中文名，用于标题展示 */
  highlightMuscleName?: string
}>()

// Emits 不变
defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'start', actionId: string, actionName: string): void
}>()
```

---

## 四、后端 API 改动

### 4.1 看板接口扩展：返回二级肌肉训练痕迹

**文件**：`GymWorkoutRecordServiceImpl.getDashboard()`

在 `MuscleGroupStatusVO` 中新增嵌套结构：

```java
@Data
@Builder
public static class SubMuscleStatus {
    private String muscleCode;        // 如 CHEST_MAJOR
    private String muscleName;        // 如 胸大肌
    private boolean trainedThisWeek;  // 本周是否有训练记录
}
```

`MuscleGroupStatusVO` 新增字段：
```java
private List<SubMuscleStatus> subMuscles;
```

**查询逻辑**（在 `getDashboard` 循环内追加）：
```java
// 对每个 muscleGroup，查出其下所有二级肌肉
List<GymMuscle> subMuscles = muscleMapper.selectList(
    new LambdaQueryWrapper<GymMuscle>()
        .eq(GymMuscle::getMuscleGroup, groupCode)
        .orderByAsc(GymMuscle::getSortNo)
);

// 对每个二级肌肉，查本周是否有训练记录
for (GymMuscle sub : subMuscles) {
    // 先找到该肌肉关联的所有 actionId
    List<GymActionMuscleRel> rels = actionMuscleRelMapper.selectList(
        new LambdaQueryWrapper<GymActionMuscleRel>()
            .eq(GymActionMuscleRel::getMuscleId, sub.getId())
    );
    Set<String> actionIds = rels.stream()
        .map(GymActionMuscleRel::getActionId)
        .collect(Collectors.toSet());
    
    boolean trained = false;
    if (!actionIds.isEmpty()) {
        Long count = recordMapper.selectCount(
            new LambdaQueryWrapper<GymWorkoutRecord>()
                .eq(GymWorkoutRecord::getUserId, userId)
                .eq(GymWorkoutRecord::getMuscleGroup, groupCode)
                .in(GymWorkoutRecord::getActionId, actionIds)
                .ge(GymWorkoutRecord::getStartTime, weekStart)
        );
        trained = count > 0;
    }
    // ...
}
```

> ⚠️ 性能注意：二级肌肉总数约 18 条，每个 group 下 2~3 条，调用量可控。若后续数据量大，可改用一条 JOIN SQL 批量查询。

### 4.2 新增动作-肌肉关联批量查询接口

**文件**：`GymActionMuscleRelController.java`（如不存在则新建）

```java
@RestController
@RequestMapping("/api/gym-action-muscle-rel")
public class GymActionMuscleRelController {
    
    /** 按大肌群批量获取所有动作-肌肉关联 */
    @GetMapping("/by-muscle-group/{muscleGroup}")
    public Result<List<GymActionMuscleRel>> listByMuscleGroup(@PathVariable String muscleGroup) {
        // 1. 查出该肌群下所有 muscleId
        // 2. 用 muscleId IN (...) 查出所有 rel
        // 3. 返回
    }
}
```

**前端 API 文件**：`web-fit-vue/src/api/gymActionMuscleRel.ts` 追加：

```ts
/** GET /api/gym-action-muscle-rel/by-muscle-group/{muscleGroup} */
export function listRelByMuscleGroup(muscleGroup: string): Promise<ApiResult<GymActionMuscleRel[]>> {
  return request.get(`/gym-action-muscle-rel/by-muscle-group/${encodeURIComponent(muscleGroup)}`)
}
```

**用途**：前端 `ActionSelectDialog` 中一次性获取该肌群下所有动作的肌肉关联关系，用于本地排序+高亮，避免对每个动作单独调 `listByActionId`（N+1 问题）。

---

## 五、数据流总览

```
┌─────────────────────────────────────────────────────────────┐
│                    GymWorkoutView.vue                       │
│                                                             │
│  onMounted                                                  │
│    ├── getDashboard()  →  DashboardVO                       │
│    │     └── muscleGroups[]: { muscleGroup, status,          │
│    │           subMuscles[]: { muscleCode, muscleName,       │
│    │                          trainedThisWeek } }            │
│    └── listAllGymMuscle() → GymMuscle[]（备用于向下传递）      │
│                                                             │
│  渲染                                                       │
│    └── <MuscleDashboard                                     │
│          :muscleGroups          ← dashboard.muscleGroups    │
│          :muscleWeeklyFlags     ← 从 subMuscles 提取         │
│          @selectMuscle          → handleMuscleSelectDetail  │
│        />                                                  │
│                                                             │
│  handleMuscleSelectDetail(muscle)                           │
│    → 打开 <ActionSelectDialog                               │
│        :muscleGroup            ← muscle.muscleGroup         │
│        :highlightMuscleCode    ← muscle.muscleCode          │
│        :highlightMuscleName    ← muscle.muscleName          │
│      />                                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 六、实施步骤（Web 端）

| 步骤 | 内容 | 预估 |
|------|------|------|
| **Step 1** | 后端：`MuscleGroupStatusVO` 追加 `subMuscles` 字段 + `getDashboard` 内部追加二级肌肉统计 | 中 |
| **Step 2** | 后端：新增 `GET /api/gym-action-muscle-rel/by-muscle-group/{group}` 接口 | 小 |
| **Step 3** | 前端 API：`gymWorkout.ts` 追加 `SubMuscleStatus` 类型；`gymActionMuscleRel.ts` 追加 `listRelByMuscleGroup` | 小 |
| **Step 4** | 前端组件：**重写** `MuscleDashboard.vue`（去掉折叠，改为平铺卡片+肌肉行列表） | 中 |
| **Step 5** | 前端组件：**重写** `ActionSelectDialog.vue`（支持 `highlightMuscleCode` 排序+高亮区域） | 中 |
| **Step 6** | 前端页面：精简 `GymWorkoutView.vue`（去掉 `handleMuscleSelect`，调整 props 传递） | 小 |
| **Step 7** | 联调 + 视觉微调 | 小 |

**建议执行顺序**：Step 1 → Step 2 → Step 3 → Step 4 → Step 5 → Step 6 → Step 7

---

## 七、关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 二级肌肉 🔥 痕迹数据来源 | **后端扩展** `MuscleGroupStatusVO` | 一次 API 返回，前端无额外计算，数据一致性好 |
| 动作排序+高亮 | **前端本地排序** | 动作数量少（每个肌群<20个），前端排序灵活且无需额外后端参数 |
| 是否需要横向 Tab 切换二级肌肉 | **一期不做，预留接口** | 核心功能是排序+高亮，Tab 切换是体验增强，可后续迭代 |
| 是否保留大肌群整体入口 | **移除** | 统一走肌肉级入口，方案B已包含全群动作 |
| 卡片展开/折叠交互 | **移除** | 二级肌肉行少（2~4行），始终展开不占过多空间 |

---

## 八、视觉参考

### 肌群卡片配色

| 肌群 | Emoji | 主题色 |
|------|-------|--------|
| 胸部 CHEST | 💪 | `#f56c6c` |
| 背部 BACK | 🦾 | `#e6a23c` |
| 肩部 SHOULDER | 🏋️ | `#409eff` |
| 手臂 ARM | 💪 | `#67c23a` |
| 腿部 LEG | 🦵 | `#b37feb` |
| 臀部 GLUTE | 🍑 | `#ff85c0` |
| 核心 CORE | 🧘 | `#36cfc9` |

> 卡片左侧可加 3px 宽的色条（`border-left`），与肌群主题色对应，增强视觉区分度。
