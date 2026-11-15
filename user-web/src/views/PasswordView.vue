<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import PageHeader from '@/components/PageHeader.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const submitting = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const forced = computed(() => route.query.forced === '1' || auth.mustChangePassword)
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,20}$/

const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!passwordPattern.test(value)) callback(new Error('新密码需 8–20 位且同时包含字母与数字'))
        else if (value === form.oldPassword) callback(new Error('新密码不得与原密码相同'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.newPassword) callback(new Error('两次输入的新密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await authApi.changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
    })
    ElMessage.success('密码已修改，请使用新密码重新登录')
    auth.clear()
    router.replace('/login')
  } catch {
    /* 错误提示由请求拦截器统一处理 */
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="修改密码" :back="!forced" />
    <div class="page-body">
      <el-alert
        v-if="forced"
        type="warning"
        :closable="false"
        show-icon
        title="当前账号需要先修改密码"
        description="使用临时密码或管理员重置密码登录后，需要设置新密码才能继续使用其他功能。"
        style="margin-bottom: 14px"
      />

      <div class="card">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入原密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="form.newPassword" type="password" show-password placeholder="8–20 位，需含字母与数字" />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
          </el-form-item>
          <el-button type="primary" size="large" round style="width: 100%" :loading="submitting" @click="submit">
            确认修改
          </el-button>
        </el-form>
      </div>

      <p class="muted small" style="margin-top: 14px; text-align: center">
        修改成功后所有已登录设备都会退出，需要重新登录
      </p>
    </div>
  </div>
</template>
