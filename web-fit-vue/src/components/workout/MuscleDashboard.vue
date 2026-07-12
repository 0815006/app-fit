<script setup lang="ts">
import type { MuscleGroupStatusVO, SubMuscleStatus } from '@/api/gymWorkout'

defineProps<{
  muscleGroups: MuscleGroupStatusVO[]
  loading: boolean
}>()

const emit = defineEmits<{
  /** 从具体肌肉入口进入，携带 muscleGroup / muscleCode / muscleName */
  (e: 'selectMuscle', payload: { muscleGroup: string; muscleCode: string; muscleName: string }): void
}>()

/** 恢复中剩余时间的格式化展示 */
function formatRemaining(seconds: number): string {
  if (seconds <= 0) return ''
  const h = Math.floor(seconds / 3600)
  if (h >= 24) {
    const d = Math.floor(h / 24)
    return `⏳ ${d}天`
  }
  if (h > 0) return `⏳ ${h}h`
  const m = Math.floor((seconds % 3600) / 60)
  return `⏳ ${m}m`
}

const GROUP_ICONS: Record<string, string> = {
  CHEST: '💪',
  BACK: '🦾',
  SHOULDER: '🏋️',
  ARM: '💪',
  LEG: '🦵',
  GLUTE: '🍑',
  CORE: '🧘',
  CARDIO: '🏃',
  FULL_BODY: '🏃',
}

/** 肌群左侧色条颜色 */
const GROUP_COLORS: Record<string, string> = {
  CHEST: '#f56c6c',
  BACK: '#e6a23c',
  SHOULDER: '#409eff',
  ARM: '#67c23a',
  LEG: '#b37feb',
  GLUTE: '#ff85c0',
  CORE: '#36cfc9',
  CARDIO: '#f39c12',
  FULL_BODY: '#909399',
}

function handleSubMuscleClick(groupCode: string, sub: SubMuscleStatus): void {
  emit('selectMuscle', {
    muscleGroup: groupCode,
    muscleCode: sub.muscleCode,
    muscleName: sub.muscleName,
  })
}
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <el-row :gutter="16">
      <el-col
        v-for="group in muscleGroups"
        :key="group.muscleGroup"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
      >
        <!-- ============ 肌群卡片（新布局） ============ -->
        <el-card
          class="muscle-card"
          :class="{ 'card-recovering': group.status === 'RECOVERING' }"
          :style="{ borderLeft: `4px solid ${GROUP_COLORS[group.muscleGroup] || '#dcdfe6'}` }"
          shadow="hover"
        >
          <!-- 卡片头部：左肌群名 + 右恢复状态 -->
          <div class="card-header">
            <div class="card-left">
              <span class="muscle-icon">{{ GROUP_ICONS[group.muscleGroup] || '🏋️' }}</span>
              <span class="muscle-name">{{ group.muscleGroupName }}</span>
              <span v-if="group.weeklyCount > 0" class="weekly-badge">
                🔥×{{ group.weeklyCount }}
              </span>
            </div>
            <div class="card-right">
              <el-tag
                v-if="group.status === 'READY'"
                type="success"
                size="small"
                effect="plain"
                class="status-tag"
              >
                🟢 可练
              </el-tag>
              <el-tag
                v-else
                type="warning"
                size="small"
                effect="plain"
                class="status-tag"
              >
                {{ formatRemaining(group.remainingSeconds) }}
              </el-tag>
            </div>
          </div>

          <!-- 分割线 -->
          <div class="card-divider" />

          <!-- 二级肌肉列表（始终展开） -->
          <div class="sub-muscle-list">
            <div
              v-for="sub in group.subMuscles"
              :key="sub.muscleCode"
              class="sub-muscle-row"
              @click="handleSubMuscleClick(group.muscleGroup, sub)"
            >
              <span class="sub-muscle-dot" />
              <span class="sub-muscle-name">{{ sub.muscleName }}</span>
              <span v-if="sub.trainedThisWeek" class="sub-muscle-fire" title="本周已训练">🔥</span>
            </div>
            <div v-if="group.subMuscles.length === 0" class="sub-muscle-empty">
              暂无细分肌肉数据
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && muscleGroups.length === 0" description="暂无肌群数据" />
  </div>
</template>

<style scoped>
.dashboard {
  padding: 4px;
}

/* ========= 卡片整体 ========= */
.muscle-card {
  transition: all 0.3s;
  margin-bottom: 12px;
  border-radius: 10px;
}

.muscle-card:hover {
  transform: translateY(-2px);
}

.card-recovering {
  opacity: 0.6;
  background-color: #f5f5f5;
}

.card-recovering:hover {
  opacity: 0.75;
}

/* ========= 卡片头部 ========= */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 6px;
}

.card-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.muscle-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.muscle-name {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  flex-shrink: 0;
}

.weekly-badge {
  font-size: 12px;
  color: #e6a23c;
  font-weight: 600;
  background: rgba(230, 162, 60, 0.1);
  padding: 2px 8px;
  border-radius: 10px;
  flex-shrink: 0;
}

.card-right {
  flex-shrink: 0;
}

.status-tag {
  font-size: 12px;
}

/* ========= 分割线 ========= */
.card-divider {
  border-top: 1px solid #ebeef5;
  margin: 8px 0 4px;
}

/* ========= 二级肌肉列表 ========= */
.sub-muscle-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sub-muscle-row {
  display: flex;
  align-items: center;
  padding: 7px 10px;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s;
}

.sub-muscle-row:hover {
  background: rgba(64, 158, 255, 0.06);
}

.sub-muscle-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c0c4cc;
  margin-right: 10px;
  flex-shrink: 0;
}

.sub-muscle-name {
  flex: 1;
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.sub-muscle-fire {
  font-size: 14px;
  flex-shrink: 0;
  margin-left: 6px;
}

.sub-muscle-empty {
  padding: 8px 10px;
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
}
</style>
