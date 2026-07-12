<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useAuth } from '@/stores/auth'

const { currentUser, initFromStorage } = useAuth()

onMounted(() => {
  if (!currentUser.value) {
    initFromStorage()
  }
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了 🌙'
  if (hour < 11) return '早上好 ☀️'
  if (hour < 14) return '中午好 🌤️'
  if (hour < 18) return '下午好 🌈'
  return '晚上好 🌆'
})

const quickLinks = [
  { path: '/gym-checkin', title: '健身打卡', desc: '开始训练，记录组数，追踪肌肉恢复', icon: '⏱️', color: '#e6a23c' },
  { path: '/ranking', title: '健身榜单', desc: '坚持榜 · 容量榜 · 1RM巅峰榜 · 进步榜', icon: '🏆', color: '#409eff' },
  { path: '/gym-maintenance', title: '健身维护', desc: '动作库、器械、肌群数据维护', icon: '💪', color: '#f56c6c' },
  { path: '/canteen-menu', title: '食堂菜单', desc: '查看各食堂每日菜品', icon: '🍽️', color: '#67c23a' },
  { path: '/meeting-room', title: '会议预定', desc: '会议室在线预定与管理', icon: '📅', color: '#9b59b6' },
  { path: '/login-stats', title: '登录统计', desc: '登录次数统计 · 用户列表 · 技术栈', icon: '📊', color: '#909399' },
]
</script>

<template>
  <div class="welcome-container">
    <div class="welcome-hero">
      <div class="hero-avatar">
        <span class="avatar-emoji">🏋️</span>
      </div>
      <h1 class="hero-title">{{ greeting }}，欢迎回来</h1>
      <div class="user-info-col">
        <div class="user-info-item">
          <span class="user-info-label">用户名</span>
          <span class="user-info-value">{{ currentUser?.username || '-' }}</span>
        </div>
        <div class="user-info-item">
          <span class="user-info-label">姓名</span>
          <span class="user-info-value">{{ currentUser?.empName || '-' }}</span>
        </div>
        <div class="user-info-item">
          <span class="user-info-label">工号</span>
          <span class="user-info-value">{{ currentUser?.empNo || '-' }}</span>
        </div>
      </div>
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

.user-info-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
}

.user-info-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-info-label {
  font-size: 13px;
  color: #909399;
  min-width: 48px;
  text-align: right;
}

.user-info-value {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
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
