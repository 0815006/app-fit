<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { HomeFilled, TrendCharts, Tools, ForkSpoon, Cpu, Calendar, Timer } from '@element-plus/icons-vue'
import type { Component } from 'vue'

interface MenuItem {
  path: string
  title: string
  icon: Component
}

const router = useRouter()
const route = useRoute()

const menuItems: MenuItem[] = [
  { path: '/', title: '首页', icon: HomeFilled },
  { path: '/health', title: '健身榜单', icon: TrendCharts },
  // { path: '/workout', title: '运动记录', icon: Trophy },
  { path: '/gym-workout', title: '健身打卡', icon: Timer },
  { path: '/gym-library', title: '健身维护', icon: Tools },
  { path: '/canteen-menu', title: '食堂菜单', icon: ForkSpoon },
  // { path: '/trip-check', title: '出行清单', icon: Suitcase },
  { path: '/meeting-room', title: '会议预定', icon: Calendar },
  { path: '/tech-stack', title: '登录统计', icon: Cpu },
]

const activeMenu = ref<string>(route.path)

// Sync sidebar highlight when route changes externally (e.g. HomeView quick links)
watch(() => route.path, (newPath) => {
  activeMenu.value = newPath
})

function handleSelect(index: string): void {
  activeMenu.value = index
  router.push(index)
}
</script>

<template>
  <aside class="side-menu">
    <div class="menu-logo">
      <span class="logo-icon">🏆</span>
      <span class="logo-text">Fit 个人健身</span>
    </div>
    <el-menu
      :default-active="activeMenu"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
      router
      @select="handleSelect"
    >
      <el-menu-item
        v-for="item in menuItems"
        :key="item.path"
        :index="item.path"
      >
        <el-icon>
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.title }}</span>
      </el-menu-item>
    </el-menu>
  </aside>
</template>

<style scoped>
.side-menu {
  height: 100%;
  background-color: #304156;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.menu-logo {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-icon {
  font-size: 22px;
  margin-right: 10px;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  white-space: nowrap;
}

:deep(.el-menu) {
  border-right: none;
  flex: 1;
  overflow-y: auto;
}

:deep(.el-menu-item) {
  font-size: 14px;
}

:deep(.el-menu-item .el-icon) {
  font-size: 16px;
  margin-right: 8px;
}
</style>
