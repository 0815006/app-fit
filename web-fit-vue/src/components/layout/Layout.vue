<script setup lang="ts">
import HeaderBar from './HeaderBar.vue'
import SideMenu from './SideMenu.vue'
import StatusBar from './StatusBar.vue'
</script>

<template>
  <div class="layout-wrapper">
    <div class="layout-sidebar">
      <SideMenu />
    </div>
    <div class="layout-header">
      <HeaderBar />
    </div>
    <div class="layout-main">
      <router-view />
    </div>
    <div class="layout-status-bar">
      <StatusBar />
    </div>
  </div>
</template>

<style scoped>
.layout-wrapper {
  display: grid;
  grid-template-columns: 240px 1fr;
  grid-template-rows: auto 1fr 34px;
  grid-template-areas:
    "sidebar header"
    "sidebar main"
    "status-bar status-bar";
  height: 100dvh;
  width: 100%;
  overflow: hidden;
}

.layout-sidebar {
  grid-area: sidebar;
  overflow: hidden;
}

.layout-header {
  grid-area: header;
  overflow: hidden;
}

.layout-main {
  grid-area: main;
  overflow: auto;
  background: #f0f2f5;
}

.layout-status-bar {
  grid-area: status-bar;
  overflow: hidden;
}
</style>
