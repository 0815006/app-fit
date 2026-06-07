import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import HealthView from '@/views/HealthView.vue'
import WorkoutView from '@/views/WorkoutView.vue'
import CanteenMenuView from '@/views/CanteenMenuView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/health',
      name: 'health',
      component: HealthView,
    },
    {
      path: '/workout',
      name: 'workout',
      component: WorkoutView,
    },
    {
      path: '/canteen-menu',
      name: 'canteenMenu',
      component: CanteenMenuView,
    },
  ],
})

export default router
