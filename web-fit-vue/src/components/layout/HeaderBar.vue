<script setup lang="ts">
import { ref } from 'vue'
import { currentEmpNo, setEmpNo } from '@/utils/currentUser'

const inputValue = ref<string>(currentEmpNo.value)

function handleSwitch(): void {
  const trimmed = inputValue.value.trim()
  if (!/^\d{7}$/.test(trimmed)) {
    return
  }
  setEmpNo(trimmed)
}
</script>

<template>
  <header class="header-bar">
    <div class="header-left">
      <h1 class="app-title">🏋️ 健康管理从我做起</h1>
    </div>
    <div class="header-right">
      <span class="header-tag">Java 21 + Vue 3 + 微信原生小程序</span>
      <el-divider direction="vertical" />
      <div class="user-switch-area">
        <el-input
          v-model="inputValue"
          placeholder="7位工号"
          maxlength="7"
          size="small"
          class="emp-input"
          clearable
          @keyup.enter="handleSwitch"
        />
        <el-button type="primary" size="small" @click="handleSwitch">切换</el-button>
        <el-tag
          v-if="currentEmpNo !== '0000000'"
          type="success"
          size="small"
          effect="dark"
        >
          {{ currentEmpNo }}
        </el-tag>
        <el-tag v-else type="info" size="small" effect="dark">未设置</el-tag>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 100%;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
}

.app-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-tag {
  font-size: 12px;
  color: #909399;
  background: #f4f4f5;
  padding: 3px 10px;
  border-radius: 4px;
}

.user-switch-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.emp-input {
  width: 100px;
}

:deep(.el-divider--vertical) {
  height: 1.4em;
  margin: 0 4px;
}
</style>
