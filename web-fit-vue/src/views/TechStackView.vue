<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { currentEmpNo } from '@/utils/currentUser'
import { createLoginRecord, getLoginCount } from '@/api/loginRecord'

interface TechItem {
  category: string
  name: string
  version: string
}

const techStack: TechItem[] = [
  { category: '后端框架', name: 'Spring Boot', version: '3.4+' },
  { category: '后端语言', name: 'Java', version: '21' },
  { category: 'ORM', name: 'MyBatis Plus', version: '3.5.11' },
  { category: '数据库', name: 'MySQL', version: '8.4 LTS' },
  { category: '数据库迁移', name: 'Flyway', version: 'Latest' },
  { category: 'Web框架', name: 'Vue', version: '3.5+' },
  { category: '构建工具', name: 'Vite', version: '6.x' },
  { category: 'Web语言', name: 'TypeScript', version: '5.7+' },
  { category: 'Web UI库', name: 'Element Plus', version: '2.9+' },
  { category: '小程序框架', name: '微信原生', version: 'Latest' },
  { category: '小程序语言', name: 'TypeScript', version: '5.x' },
  { category: '小程序 UI库', name: 'TDesign Miniprogram', version: 'Latest' },
]

const loginCount = ref<number | null>(null)
const loading = ref<boolean>(false)

async function loadLoginData(): Promise<void> {
  const empNo = currentEmpNo.value
  loading.value = true
  try {
    await createLoginRecord('WEB')
    const result = await getLoginCount(empNo)
    loginCount.value = result.data
  } catch {
    loginCount.value = null
  } finally {
    loading.value = false
  }
}

watch(currentEmpNo, () => {
  loadLoginData()
})

onMounted(() => {
  loadLoginData()
})
</script>

<template>
  <div class="tech-container">
    <el-row :gutter="24">
      <el-col :xs="24" :sm="12">
        <el-card shadow="hover" v-loading="loading">
          <template #header>
            <span class="card-header-title">📊 登录统计</span>
          </template>
          <div class="stat-content">
            <p class="stat-label">工号: <el-tag type="primary">{{ currentEmpNo }}</el-tag></p>
            <p class="stat-label">登录来源: <el-tag>网页端 (WEB)</el-tag></p>
            <el-divider />
            <div class="stat-number">
              <span class="count-number">{{ loginCount ?? '-' }}</span>
              <span class="count-unit">次</span>
            </div>
            <p class="stat-hint">每次刷新页面自动记录一次登录</p>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12">
        <el-card shadow="hover">
          <template #header>
            <span class="card-header-title">🛠️ 技术选型</span>
          </template>
          <el-table :data="techStack" stripe size="small" max-height="400">
            <el-table-column prop="category" label="分类" width="110" />
            <el-table-column prop="name" label="技术" width="150" />
            <el-table-column prop="version" label="版本" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.tech-container {
  padding: 24px;
}

.card-header-title {
  font-weight: 600;
}

.stat-content {
  text-align: center;
}

.stat-label {
  margin: 8px 0;
  font-size: 14px;
}

.stat-number {
  margin: 16px 0;
}

.count-number {
  font-size: 48px;
  font-weight: 700;
  color: #409eff;
}

.count-unit {
  font-size: 18px;
  color: #909399;
  margin-left: 4px;
}

.stat-hint {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
