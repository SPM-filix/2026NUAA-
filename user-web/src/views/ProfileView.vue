<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fileApi, userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import PageHeader from '@/components/PageHeader.vue'

const auth = useAuthStore()
const formRef = ref()
const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)
const phone = ref('')
const form = reactive({ nickname: '', avatarUrl: '', address: '' })

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 20, message: '昵称最多 20 个字符', trigger: 'blur' },
  ],
  address: [{ max: 100, message: '地址备注不能超过 100 个字符', trigger: 'blur' }],
}

async function handleUpload(options) {
  const file = options.file
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.error('仅支持 jpg / png / webp 图片')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片不能超过 2 MB')
    return
  }
  uploading.value = true
  try {
    const data = await fileApi.upload(file, 'AVATAR')
    form.avatarUrl = data.url
    ElMessage.success('头像已上传，保存后生效')
  } catch {
    /* 错误提示由请求拦截器统一处理 */
  } finally {
    uploading.value = false
  }
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const user = await userApi.updateMe({
      nickname: form.nickname,
      avatarUrl: form.avatarUrl || undefined,
      address: form.address,
    })
    auth.user = user
    ElMessage.success('资料已保存')
  } catch {
    /* 错误提示由请求拦截器统一处理 */
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    const user = await auth.fetchMe()
    phone.value = user.phone
    form.nickname = user.nickname
    form.avatarUrl = user.avatarUrl || ''
    form.address = user.address || ''
  } catch {
    /* 错误提示由请求拦截器统一处理 */
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <PageHeader title="个人资料" />
    <div class="page-body">
      <el-skeleton v-if="loading" :rows="5" animated />

      <template v-else>
        <div class="card avatar-card">
          <el-upload
            :show-file-list="false"
            :http-request="handleUpload"
            accept="image/jpeg,image/png,image/webp"
          >
            <el-avatar :size="76" :src="form.avatarUrl">
              {{ (form.nickname || '绿').slice(0, 1) }}
            </el-avatar>
            <div class="avatar-tip">{{ uploading ? '上传中…' : '点击更换头像' }}</div>
          </el-upload>
          <p class="muted small">支持 jpg / png / webp，不超过 2 MB</p>
        </div>

        <div class="card" style="margin-top: 14px">
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" maxlength="20" show-word-limit />
            </el-form-item>
            <el-form-item label="地址备注" prop="address">
              <el-input v-model="form.address" type="textarea" :rows="2" maxlength="100" show-word-limit placeholder="便于回收员联系你，如 3 号楼 502" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input :model-value="phone" disabled />
              <div class="form-tip" style="margin-top: 6px">手机号为登录账号，如需修改请联系管理员</div>
            </el-form-item>
            <el-button type="primary" size="large" round style="width: 100%" :loading="saving" @click="save">
              保存修改
            </el-button>
          </el-form>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.avatar-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px;
  text-align: center;
}

.avatar-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--brand);
}
</style>
