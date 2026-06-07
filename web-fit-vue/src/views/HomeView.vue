<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getEmpNo } from '@/utils/currentUser'
import { createLoginRecord, getLoginCount } from '@/api/loginRecord'
import UserSwitcher from '@/components/user/UserSwitcher.vue'

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
const currentEmpNo = ref<string>(getEmpNo())

async function loadLoginData(): Promise<void> {
  const empNo = getEmpNo()
  currentEmpNo.value = empNo
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

function onUserSwitched(empNo: string): void {
  currentEmpNo.value = empNo
  loadLoginData()
}

onMounted(() => {
  loadLoginData()
})
</script>

<template>
  <div class="home-container">
    <el-container>
      <el-header class="app-header">
        <h1>🏋️ Fit 全栈稳健版</h1>
        <p class="subtitle">Java 21 + Vue 3 + 微信原生小程序</p>
      </el-header>

      <el-main>
        <UserSwitcher @switched="onUserSwitched" />

        <el-row :gutter="24">
          <!-- Login Count Card -->
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

          <!-- Tech Stack Card -->
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
      </el-main>

      <el-footer class="app-footer">
        <span>© 2026 Fit Project · 全栈稳健版</span>
      </el-footer>
    </el-container>
  </div>
</template>

<style scoped>
.home-container {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 16px;
}

.app-header {
  text-align: center;
  padding: 32px 0 8px;
}
.app-header h1 {
  margin: 0;
  font-size: 28px;
}
.subtitle {
  color: #909399;
  margin: 8px 0 0;
  font-size: 14px;
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

.app-footer {
  text-align: center;
  padding: 24px 0;
  color: #c0c4cc;
  font-size: 13px;
}
</style>
