<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuth } from '@/stores/auth'

const router = useRouter()
const { login, loading } = useAuth()

const formRef = ref()
const form = ref({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    await login(form.value.username, form.value.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch {
    ElMessage.error('账号或密码错误')
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="logo">💪</div>
        <h1>App-Fit</h1>
        <p class="subtitle">全栈稳健版 · 管理后台</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            autocomplete="current-password"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="copyright-footer">
      <p class="copyright-line">
        Copyright &copy; 2026 realapex.site 个人健身记录网站 All Rights Reserved. RealMadrid 版权所有
      </p>
      <p class="copyright-line">
        <a class="icp-link" href="https://beian.miit.gov.cn" target="_blank" rel="noopener">ICP备案/许可证号：沪ICP备2026003602号-2</a>
      </p>
      <p class="copyright-line">
        <a class="icp-link" href="https://beian.mps.gov.cn" target="_blank" rel="noopener">
          <img class="police-icon" src="/police_logo.png" alt="公安备案" width="14" height="14" />
          沪公网安备31011502406692号
        </a>
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  font-size: 48px;
  margin-bottom: 8px;
}

.login-header h1 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 4px;
}

.subtitle {
  font-size: 13px;
  color: #909399;
}

.login-btn {
  width: 100%;
}

.copyright-footer {
  margin-top: 24px;
  text-align: center;
}

.copyright-line {
  margin: 2px 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.8;
}

.icp-link {
  color: rgba(255, 255, 255, 0.6);
  text-decoration: none;
  font-size: 12px;
  transition: color 0.2s;
}

.icp-link:hover {
  color: #fff;
}

.police-icon {
  display: inline-block;
  vertical-align: -2px;
  margin-right: 2px;
}
</style>
