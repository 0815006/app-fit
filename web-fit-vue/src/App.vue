<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuth } from '@/stores/auth'
import Layout from '@/components/layout/Layout.vue'

const { isLoggedIn, initFromStorage } = useAuth()
const ready = ref(false)

onMounted(async () => {
  await initFromStorage()
  ready.value = true
})
</script>

<template>
  <div v-if="!ready" class="loading-screen">
    <span>加载中...</span>
  </div>
  <Layout v-else-if="isLoggedIn" />
  <router-view v-else />
</template>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body { height: 100%; overflow: hidden; }

.loading-screen {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  color: #909399;
  font-size: 16px;
}
</style>
