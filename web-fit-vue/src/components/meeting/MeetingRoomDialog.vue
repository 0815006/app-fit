<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { createMeetingRoom, updateMeetingRoom, type MeetingRoom } from '@/api/meetingRoom'

const props = defineProps<{
  visible: boolean
  editData: MeetingRoom | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'saved'): void
}>()

const isEdit = computed(() => props.editData !== null)

const facilitiesOptions = ['投影仪', '白板', '视频会议设备', '电话', '音响系统', '打印机']

const form = ref<Record<string, string | number>>({
  name: '',
  location: '',
  capacity: 10,
  facilities: '[]',
  photoUrl: '',
  sortOrder: 0,
})

const selectedFacilities = ref<string[]>([])
const saving = ref(false)

function resetForm(): void {
  if (props.editData) {
    form.value.name = props.editData.name
    form.value.location = props.editData.location
    form.value.capacity = props.editData.capacity
    form.value.photoUrl = props.editData.photoUrl || ''
    form.value.sortOrder = props.editData.sortOrder || 0
    selectedFacilities.value = JSON.parse(props.editData.facilities || '[]')
  } else {
    form.value.name = ''
    form.value.location = ''
    form.value.capacity = 10
    form.value.photoUrl = ''
    form.value.sortOrder = 0
    selectedFacilities.value = []
  }
}

watch(() => props.visible, (val) => {
  if (val) resetForm()
})

function toggleFacility(f: string): void {
  const idx = selectedFacilities.value.indexOf(f)
  if (idx >= 0) {
    selectedFacilities.value.splice(idx, 1)
  } else {
    selectedFacilities.value.push(f)
  }
}

async function handleSave(): Promise<void> {
  if (!form.value.name || !form.value.location) {
    ElMessage.warning('请填写会议室名称和位置')
    return
  }
  saving.value = true
  try {
    const data = {
      name: form.value.name as string,
      location: form.value.location as string,
      capacity: form.value.capacity as number,
      facilities: JSON.stringify(selectedFacilities.value),
      photoUrl: form.value.photoUrl as string,
      sortOrder: form.value.sortOrder as number,
    }
    if (isEdit.value) {
      await updateMeetingRoom(props.editData!.id, data)
      ElMessage.success('会议室已更新')
    } else {
      await createMeetingRoom(data)
      ElMessage.success('会议室已创建')
    }
    emit('saved')
    emit('update:visible', false)
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

function handleClose(): void {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑会议室' : '新增会议室'"
    width="520px"
    :close-on-click-modal="false"
    @update:model-value="handleClose"
  >
    <el-form label-width="80px" @submit.prevent="handleSave">
      <el-form-item label="名称" required>
        <el-input v-model="form.name" placeholder="如：会议室 A (301)" />
      </el-form-item>
      <el-form-item label="位置" required>
        <el-input v-model="form.location" placeholder="如：3楼 301室" />
      </el-form-item>
      <el-form-item label="座位数">
        <el-input-number v-model="form.capacity" :min="1" :max="100" />
      </el-form-item>
      <el-form-item label="配套设施">
        <el-checkbox-group v-model="selectedFacilities">
          <el-checkbox v-for="f in facilitiesOptions" :key="f" :label="f" :value="f" />
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="照片URL">
        <el-input v-model="form.photoUrl" placeholder="会议室实景照片的URL" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">
        {{ isEdit ? '保存修改' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>
