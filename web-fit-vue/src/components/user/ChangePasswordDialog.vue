<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changePassword } from '@/api/auth'

defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const formRef = ref()
const form = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (err?: Error) => void) => {
        if (value !== form.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    await changePassword(form.value.oldPassword, form.value.newPassword)
    ElMessage.success('密码修改成功')
    emit('close')
  } catch {
    ElMessage.error('旧密码不正确')
  }
}

function handleClose() {
  formRef.value?.resetFields()
  emit('close')
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="修改密码"
    width="420px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
    >
      <el-form-item label="旧密码" prop="oldPassword">
        <el-input
          v-model="form.oldPassword"
          type="password"
          placeholder="请输入旧密码"
          show-password
        />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          placeholder="至少6位"
          show-password
        />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="再次输入新密码"
          show-password
          @keyup.enter="handleSubmit"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('close')">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确认修改</el-button>
    </template>
  </el-dialog>
</template>
