import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import RankingView from '@/views/RankingView.vue'
import TrainingCenterView from '@/views/TrainingCenterView.vue'
import CanteenMenuView from '@/views/CanteenMenuView.vue'
import GymMaintenanceView from '@/views/GymMaintenanceView.vue'
import LoginStatsView from '@/views/LoginStatsView.vue'
import TripCheckView from '@/views/TripCheckView.vue'
import MeetingRoomView from '@/views/MeetingRoomView.vue'
import LoginView from '@/views/LoginView.vue'
import GymCheckinView from '@/views/GymCheckinView.vue'

const TOKEN_KEY = 'satoken'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    name: 'home',
    component: HomeView,
    meta: { requiresAuth: true },
  },
  {
    path: '/ranking',
    name: 'ranking',
    component: RankingView,
    meta: { requiresAuth: true },
  },
  {
    path: '/training-center',
    name: 'trainingCenter',
    component: TrainingCenterView,
    meta: { requiresAuth: true },
  },
  {
    path: '/canteen-menu',
    name: 'canteenMenu',
    component: CanteenMenuView,
    meta: { requiresAuth: true },
  },
  {
    path: '/gym-maintenance',
    name: 'gymMaintenance',
    component: GymMaintenanceView,
    meta: { requiresAuth: true },
  },
  {
    path: '/login-stats',
    name: 'loginStats',
    component: LoginStatsView,
    meta: { requiresAuth: true },
  },
  {
    path: '/trip-check',
    name: 'tripCheck',
    component: TripCheckView,
    meta: { requiresAuth: true },
  },
  {
    path: '/meeting-room',
    name: 'meetingRoom',
    component: MeetingRoomView,
    meta: { requiresAuth: true },
  },
  {
    path: '/gym-checkin',
    name: 'gymCheckin',
    component: GymCheckinView,
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局前置守卫
router.beforeEach((to, _from, next) => {
  if (to.meta.requiresAuth === false) {
    next()
    return
  }

  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) {
    next('/login')
    return
  }

  next()
})

export default router
