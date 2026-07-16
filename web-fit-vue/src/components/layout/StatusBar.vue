<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Clock, Connection } from '@element-plus/icons-vue'
import { getSystemInfo } from '@/api/system'

const currentTime = ref<string>('')
const loginIp = ref<string>('--')

let timer: ReturnType<typeof setInterval> | null = null

function formatTime(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  const ss = String(date.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`
}

function tick(): void {
  currentTime.value = formatTime(new Date())
}

async function fetchLoginIp(): Promise<void> {
  try {
    const result = await getSystemInfo()
    if (result.data?.loginIp) {
      loginIp.value = result.data.loginIp
    }
  } catch {
    loginIp.value = '--'
  }
}

onMounted(() => {
  tick()
  timer = setInterval(tick, 1000)
  fetchLoginIp()
})

onUnmounted(() => {
  if (timer !== null) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<template>
  <footer class="status-bar">
    <div class="status-row">
      <div class="status-left">
        <span class="status-item">
          <el-icon><Clock /></el-icon>
          <span class="status-label">本地时间</span>
          <span class="status-value">{{ currentTime }}</span>
        </span>
      </div>
      <div class="status-center">
        <span class="copyright-text">
          Copyright &copy; 2026 realapex.site 个人健身记录网站 All Rights Reserved. RealMadrid 版权所有 &nbsp;|&nbsp;
          <a class="icp-link" href="https://beian.miit.gov.cn" target="_blank" rel="noopener">ICP备案/许可证号：沪ICP备2026003602号-2</a>
          &nbsp;|&nbsp;
          <a class="icp-link police-link" href="https://beian.mps.gov.cn" target="_blank" rel="noopener">
            <img class="police-icon" src="/police_logo.png" alt="公安备案" width="14" height="14" />
            沪公网安备31011502406692号
          </a>
        </span>
      </div>
      <div class="status-right">
        <span class="status-item">
          <el-icon><Connection /></el-icon>
          <span class="status-label">登录 IP</span>
          <span class="status-value">{{ loginIp }}</span>
        </span>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.status-bar {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 16px;
  height: 100%;
  background: #f5f7fa;
  border-top: 1px solid #e4e7ed;
  font-size: 12px;
  color: #606266;
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.status-left,
.status-center,
.status-right {
  display: flex;
  align-items: center;
}

.status-center {
  flex: 1;
  justify-content: center;
}

.copyright-text {
  color: #909399;
  font-size: 12px;
}

.icp-link {
  color: #909399;
  text-decoration: none;
  font-size: 12px;
  transition: color 0.2s;
}

.icp-link:hover {
  color: #409eff;
}

.police-icon {
  display: inline-block;
  vertical-align: -2px;
  margin-right: 2px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-label {
  color: #909399;
  margin-left: 4px;
}

.status-value {
  color: #303133;
  font-weight: 500;
  font-family: 'Courier New', Courier, monospace;
}

:deep(.el-icon) {
  font-size: 13px;
  color: #909399;
}
</style>
