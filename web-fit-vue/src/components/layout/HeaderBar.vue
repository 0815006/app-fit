<script setup lang="ts">
import { ref } from 'vue'
import { useAuth } from '@/stores/auth'
import ChangePasswordDialog from '@/components/user/ChangePasswordDialog.vue'

const { currentUser, logout } = useAuth()
const pwdDialogVisible = ref(false)
</script>

<template>
  <div>
    <header class="header-bar">
      <div class="header-left">
        <h1 class="app-title">🏋️ 健康管理从我做起</h1>
      </div>
      <div class="header-right">
        <span class="header-tag">Java 21 + Vue 3 + 微信原生小程序</span>
        <el-divider direction="vertical" />
        <div class="user-switch-area">
          <el-dropdown trigger="click" v-if="currentUser?.empNo && currentUser.empNo !== '0000000'">
            <el-tag
              type="success"
              size="small"
              effect="dark"
              class="user-tag"
            >
              👤 {{ currentUser.empNo }}
            </el-tag>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="pwdDialogVisible = true">
                  <span>🔒 修改密码</span>
                </el-dropdown-item>
                <el-dropdown-item divided @click="logout">
                  <span style="color: #f56c6c;">🚪 退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-tag v-else type="info" size="small" effect="dark">未登录</el-tag>
        </div>
      </div>
    </header>

    <ChangePasswordDialog
      :visible="pwdDialogVisible"
      @close="pwdDialogVisible = false"
    />
  </div>
</template>

<style scoped>
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 100%;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
}

.app-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-tag {
  font-size: 12px;
  color: #909399;
  background: #f4f4f5;
  padding: 3px 10px;
  border-radius: 4px;
}

.user-switch-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-tag {
  cursor: pointer;
}

:deep(.el-divider--vertical) {
  height: 1.4em;
  margin: 0 4px;
}
</style>
