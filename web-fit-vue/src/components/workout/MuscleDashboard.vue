<script setup lang="ts">
import { ref } from 'vue'
import type { MuscleGroupStatusVO } from '@/api/gymWorkout'
import type { GymMuscle } from '@/api/gymMuscle'

const props = defineProps<{
  muscleGroups: MuscleGroupStatusVO[]
  loading: boolean
  /** 所有肌肉数据，按 muscleGroup 分组后用于第二层展示 */
  muscles?: GymMuscle[]
}>()

const emit = defineEmits<{
  (e: 'select', group: string): void
  /** 从具体肌肉入口进入 */
  (e: 'selectMuscle', muscle: GymMuscle): void
}>()

/** 当前展开的大肌群 code 集合 */
const expandedGroups = ref<Set<string>>(new Set())

function toggleExpand(groupCode: string): void {
  const next = new Set(expandedGroups.value)
  if (next.has(groupCode)) {
    next.delete(groupCode)
  } else {
    next.add(groupCode)
  }
  expandedGroups.value = next
}

function isExpanded(groupCode: string): boolean {
  return expandedGroups.value.has(groupCode)
}

/** 获取某个大肌群下的所有肌肉 */
function getMusclesForGroup(groupCode: string): GymMuscle[] {
  return (props.muscles || []).filter(m => m.muscleGroup === groupCode)
}

/** 恢复中剩余时间的格式化展示 */
function formatRemaining(seconds: number): string {
  if (seconds <= 0) return ''
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  if (h >= 24) {
    const d = Math.floor(h / 24)
    return `还剩 ${d} 天`
  }
  if (h > 0) return `还剩 ${h} 小时`
  return `还剩 ${m} 分钟`
}

const GROUP_ICONS: Record<string, string> = {
  CHEST: '💪',
  BACK: '🦾',
  SHOULDER: '🏋️',
  ARM: '💪',
  LEG: '🦵',
  GLUTE: '🍑',
  CORE: '🧘',
  FULL_BODY: '🏃',
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
        <!-- ============ 第一层：大肌群卡片 ============ -->
        <el-card
          class="muscle-card"
          :class="{ 'card-recovering': group.status === 'RECOVERING' }"
          shadow="hover"
        >
          <!-- 大肌群头部（可点击展开/收起） -->
          <div class="card-header" @click.stop="toggleExpand(group.muscleGroup)">
            <div class="card-left">
              <span class="expand-icon">{{ isExpanded(group.muscleGroup) ? '▼' : '▶' }}</span>
              <span class="muscle-icon">{{ GROUP_ICONS[group.muscleGroup] || '🏋️' }}</span>
              <div class="muscle-info">
                <span class="muscle-name">{{ group.muscleGroupName }}</span>
                <span class="weekly-count" v-if="group.weeklyCount > 0">
                  🔥 × {{ group.weeklyCount }}
                </span>
              </div>
            </div>
            <div class="card-right">
              <el-tag
                :type="group.status === 'READY' ? 'success' : 'warning'"
                size="small"
                effect="plain"
              >
                {{ group.status === 'READY' ? '🟢 已恢复' : '🟡 恢复中' }}
              </el-tag>
              <span v-if="group.status === 'RECOVERING'" class="remaining-time">
                {{ formatRemaining(group.remainingSeconds) }}
              </span>
            </div>
          </div>

          <!-- 大肌群快捷入口按钮 -->
          <div class="group-quick-entry">
            <el-button
              size="small"
              type="primary"
              plain
              @click.stop="emit('select', group.muscleGroup)"
            >
              从 {{ group.muscleGroupName }} 入口
            </el-button>
          </div>

          <!-- ============ 第二层：具体肌肉展开区 ============ -->
          <el-collapse-transition>
            <div v-show="isExpanded(group.muscleGroup)" class="muscle-sub-list">
              <div class="sub-divider" />
              <div
                v-for="muscle in getMusclesForGroup(group.muscleGroup)"
                :key="muscle.id"
                class="muscle-sub-item"
                @click.stop="emit('selectMuscle', muscle)"
              >
                <span class="sub-muscle-dot">•</span>
                <span class="sub-muscle-name">{{ muscle.muscleName }}</span>
                <el-button
                  size="small"
                  type="primary"
                  link
                  class="sub-muscle-btn"
                >
                  进入训练
                </el-button>
              </div>
              <el-empty
                v-if="getMusclesForGroup(group.muscleGroup).length === 0"
                description="暂无具体肌肉数据"
                :image-size="40"
              />
            </div>
          </el-collapse-transition>
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

.muscle-card {
  transition: all 0.3s;
  margin-bottom: 12px;
}

.muscle-card:hover {
  transform: translateY(-2px);
}

.card-recovering {
  opacity: 0.65;
  background-color: #f5f5f5;
}

/* ========= 大肌群头部 ========= */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  user-select: none;
  padding-bottom: 4px;
}

.card-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.expand-icon {
  font-size: 12px;
  color: #909399;
  width: 14px;
  text-align: center;
  flex-shrink: 0;
}

.muscle-icon {
  font-size: 28px;
}

.muscle-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.muscle-name {
  font-size: 15px;
  font-weight: 600;
}

.weekly-count {
  font-size: 13px;
  color: #e6a23c;
}

.card-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}

.remaining-time {
  font-size: 12px;
  color: #909399;
}

/* ========= 大肌群快捷入口 ========= */
.group-quick-entry {
  margin-top: 8px;
  padding-top: 4px;
}

.group-quick-entry .el-button {
  width: 100%;
}

/* ========= 第二层：具体肌肉 ========= */
.muscle-sub-list {
  margin-top: 4px;
}

.sub-divider {
  border-top: 1px dashed #e4e7ed;
  margin: 6px 0 8px;
}

.muscle-sub-item {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s;
}

.muscle-sub-item:hover {
  background: rgba(64, 158, 255, 0.08);
}

.sub-muscle-dot {
  color: #409eff;
  margin-right: 6px;
  font-weight: 700;
}

.sub-muscle-name {
  flex: 1;
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.sub-muscle-btn {
  font-size: 13px;
}
</style>
