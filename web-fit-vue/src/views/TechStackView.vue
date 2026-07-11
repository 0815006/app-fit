<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { currentEmpNo } from '@/utils/currentUser'
import { createLoginRecord, getWebStats } from '@/api/loginRecord'
import type { WebStatsDTO } from '@/typings'

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

const webStats = ref<WebStatsDTO | null>(null)
const loading = ref<boolean>(false)

async function loadLoginData(): Promise<void> {
  loading.value = true
  try {
    await createLoginRecord('WEB')
    const result = await getWebStats()
    webStats.value = result.data
  } catch {
    webStats.value = null
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
      <el-col :xs="24" :sm="14">
        <el-card shadow="hover" v-loading="loading">
          <template #header>
            <span class="card-header-title">📊 登录统计</span>
          </template>
          <div class="stat-content">
            <p class="stat-label">工号: <el-tag type="primary">{{ currentEmpNo }}</el-tag></p>
            <p class="stat-label">登录来源: <el-tag>网页端 (WEB)</el-tag></p>
            <el-divider />
            <el-row :gutter="16">
              <el-col :span="8">
                <div class="stat-card">
                  <div class="stat-card-title">我的<br/>WEB端</div>
                  <div class="stat-card-number">{{ webStats?.myWebCount ?? '-' }}</div>
                  <div class="stat-card-unit">次</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="stat-card">
                  <div class="stat-card-title">WEB端<br/>总次数</div>
                  <div class="stat-card-number">{{ webStats?.totalWebCount ?? '-' }}</div>
                  <div class="stat-card-unit">次</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="stat-card">
                  <div class="stat-card-title">小程序端<br/>总次数</div>
                  <div class="stat-card-number">{{ webStats?.totalMiniProgramCount ?? '-' }}</div>
                  <div class="stat-card-unit">次</div>
                </div>
              </el-col>
            </el-row>
            <el-row :gutter="16" style="margin-top: 12px;">
              <el-col :span="24">
                <div class="stat-card stat-card-full">
                  <div class="stat-card-title">全部总次数 (WEB + 小程序)</div>
                  <div class="stat-card-number stat-card-number-large">{{ webStats?.totalAllCount ?? '-' }}</div>
                  <div class="stat-card-unit">次</div>
                </div>
              </el-col>
            </el-row>
            <p class="stat-hint">每次刷新页面自动记录一次登录</p>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="10">
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

.stat-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px 8px;
  text-align: center;
}

.stat-card-full {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-card-full .stat-card-title,
.stat-card-full .stat-card-number,
.stat-card-full .stat-card-unit {
  color: #ffffff;
}

.stat-card-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
  line-height: 1.4;
}

.stat-card-number {
  font-size: 28px;
  font-weight: 700;
  color: #409eff;
}

.stat-card-number-large {
  font-size: 36px;
}

.stat-card-unit {
  font-size: 13px;
  color: #909399;
}

.stat-hint {
  color: #c0c4cc;
  font-size: 12px;
  margin-top: 12px;
}
</style>
