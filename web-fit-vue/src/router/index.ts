import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import HealthView from '@/views/HealthView.vue'
import WorkoutView from '@/views/WorkoutView.vue'
import CanteenMenuView from '@/views/CanteenMenuView.vue'
import GymActionLibraryView from '@/views/GymActionLibraryView.vue'
import TechStackView from '@/views/TechStackView.vue'
import TripCheckView from '@/views/TripCheckView.vue'
import MeetingRoomView from '@/views/MeetingRoomView.vue'
import LoginView from '@/views/LoginView.vue'
import GymWorkoutView from '@/views/GymWorkoutView.vue'

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
    path: '/health',
    name: 'health',
    component: HealthView,
    meta: { requiresAuth: true },
  },
  {
    path: '/workout',
    name: 'workout',
    component: WorkoutView,
    meta: { requiresAuth: true },
  },
  {
    path: '/canteen-menu',
    name: 'canteenMenu',
    component: CanteenMenuView,
    meta: { requiresAuth: true },
  },
  {
    path: '/gym-library',
    name: 'gymLibrary',
    component: GymActionLibraryView,
    meta: { requiresAuth: true },
  },
  {
    path: '/tech-stack',
    name: 'techStack',
    component: TechStackView,
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
    path: '/gym-workout',
    name: 'gymWorkout',
    component: GymWorkoutView,
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
