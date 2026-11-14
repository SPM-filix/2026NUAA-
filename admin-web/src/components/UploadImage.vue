<template>
  <div class="upload-image">
    <el-upload
      :show-file-list="false"
      :http-request="handleRequest"
      :before-upload="beforeUpload"
      accept="image/jpeg,image/png,image/webp"
      class="uploader"
    >
      <div class="frame" :class="{ 'is-filled': modelValue }">
        <img v-if="modelValue" :src="modelValue" alt="已上传图片" />
        <div v-else class="placeholder">
          <el-icon><Plus /></el-icon>
          <span>上传图片</span>
        </div>
        <div v-if="uploading" class="mask"><el-icon class="is-loading"><Loading /></el-icon></div>
      </div>
    </el-upload>
    <p class="hint">支持 jpg / png / webp，不超过 2 MB</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/file'

const props = defineProps({
  modelValue: { type: String, default: '' },
  scene: { type: String, default: 'PRODUCT' }
})

const emit = defineEmits(['update:modelValue'])
const uploading = ref(false)

function beforeUpload(file) {
  const allowed = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowed.includes(file.type)) {
    ElMessage.error('仅支持 jpg / png / webp 图片')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片不能超过 2 MB')
    return false
  }
  return true
}

async function handleRequest({ file }) {
  uploading.value = true
  try {
    const data = await uploadFile(file, props.scene)
    emit('update:modelValue', data.url)
    ElMessage.success('图片上传成功')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.upload-image {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.frame {
  position: relative;
  width: 112px;
  height: 112px;
  border: 1px dashed var(--line-strong);
  border-radius: 12px;
  background: #fafcfb;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.16s ease, background 0.16s ease;
}

.frame:hover {
  border-color: var(--brand-500);
  background: var(--brand-50);
}

.frame.is-filled {
  border-style: solid;
}

.frame img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 100%;
  color: var(--ink-400);
  font-size: 12px;
}

.placeholder .el-icon {
  font-size: 20px;
}

.mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.7);
  color: var(--brand-600);
  font-size: 22px;
}

.hint {
  color: var(--ink-400);
  font-size: 11.5px;
}
</style>
