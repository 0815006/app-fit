<script setup lang="ts">
import { ref } from 'vue'
import { getEmpNo, setEmpNo } from '@/utils/currentUser'

const emit = defineEmits<{
  switched: [empNo: string]
}>()

const currentEmpNo = ref<string>(getEmpNo())
const inputValue = ref<string>(getEmpNo())

function handleSwitch(): void {
  const trimmed = inputValue.value.trim()
  if (!/^\d{7}$/.test(trimmed)) {
    return
  }
  setEmpNo(trimmed)
  currentEmpNo.value = trimmed
  emit('switched', trimmed)
}
</script>

<template>
  <div class="user-switcher">
    <el-card shadow="hover">
      <template #header>
        <span class="card-header-title">👤 当前操作者</span>
      </template>
      <el-row :gutter="12" align="middle">
        <el-col :span="14">
          <el-input
            v-model="inputValue"
            placeholder="请输入7位工号"
            maxlength="7"
            clearable
          />
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="handleSwitch">切换用户</el-button>
        </el-col>
        <el-col :span="4">
          <el-tag v-if="currentEmpNo !== '0000000'" type="success" size="large">
            {{ currentEmpNo }}
          </el-tag>
          <el-tag v-else type="info" size="large">未设置</el-tag>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<style scoped>
.user-switcher {
  margin-bottom: 24px;
}
.card-header-title {
  font-weight: 600;
}
</style>
