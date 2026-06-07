<script setup lang="ts">
</script>

<template>
  <div class="placeholder-view">
    <el-result
      icon="info"
      title="健康数据"
      sub-title="此功能正在开发中，敬请期待..."
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.placeholder-view {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
</style>
