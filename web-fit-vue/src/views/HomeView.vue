<script setup lang="ts">
import { computed } from 'vue'
import { currentEmpNo } from '@/utils/currentUser'

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了 🌙'
  if (hour < 11) return '早上好 ☀️'
  if (hour < 14) return '中午好 🌤️'
  if (hour < 18) return '下午好 🌈'
  return '晚上好 🌆'
})

const quickLinks = [
  { path: '/health', title: '健康数据', desc: '体重、BMI 等体征指标记录', icon: '📊', color: '#409eff' },
  { path: '/canteen-menu', title: '食堂菜单', desc: '查看各食堂每日菜品', icon: '🍽️', color: '#67c23a' },
  { path: '/gym-library', title: '健身动作库', desc: '动作、器械、肌群数据管理', icon: '💪', color: '#f56c6c' },
  { path: '/tech-stack', title: '技术选型', desc: '登录统计与技术栈一览', icon: '🛠️', color: '#909399' },
]
</script>

<template>
  <div class="welcome-container">
    <div class="welcome-hero">
      <div class="hero-avatar">
        <span class="avatar-emoji">🏋️</span>
      </div>
      <h1 class="hero-title">{{ greeting }}，欢迎回来</h1>
      <p class="hero-subtitle">
        当前工号：
        <el-tag type="primary" size="large">{{ currentEmpNo }}</el-tag>
      </p>
    </div>

    <el-divider />

    <div class="quick-section">
      <h2 class="section-title">快捷入口</h2>
      <div class="quick-grid">
        <div
          v-for="link in quickLinks"
          :key="link.path"
          class="quick-card"
          :style="{ '--card-color': link.color }"
          @click="$router.push(link.path)"
        >
          <span class="quick-icon">{{ link.icon }}</span>
          <div class="quick-info">
            <span class="quick-title">{{ link.title }}</span>
            <span class="quick-desc">{{ link.desc }}</span>
          </div>
        </div>
      </div>
    </div>

    <el-divider />

    <div class="info-section">
      <el-alert
        title="Fit 健身记录系统"
        type="info"
        :closable="false"
        show-icon
        description="全栈稳健版项目 — Spring Boot 3.4 + Vue 3.5，一站式健身数据管理平台。"
      />
    </div>
  </div>
</template>

<style scoped>
.welcome-container {
  padding: 48px 40px;
  max-width: 800px;
  margin: 0 auto;
}

.welcome-hero {
  text-align: center;
  padding: 40px 0 20px;
}

.hero-avatar {
  margin-bottom: 20px;
}

.avatar-emoji {
  font-size: 72px;
  display: inline-block;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.hero-title {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 12px;
}

.hero-subtitle {
  font-size: 15px;
  color: #909399;
  margin: 0;
}

.section-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px;
}

.quick-section {
  padding: 8px 0;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.25s ease;
}

.quick-card:hover {
  border-color: var(--card-color);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.quick-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.quick-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.quick-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.quick-desc {
  font-size: 12px;
  color: #909399;
}

.info-section {
  padding: 8px 0;
}

@media (max-width: 600px) {
  .welcome-container {
    padding: 24px 16px;
  }
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
