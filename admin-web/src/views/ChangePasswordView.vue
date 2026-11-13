<template>
  <div class="password-page">
    <div class="password-shell">
      <header class="password-brand">
        <span class="mark">
          <svg viewBox="0 0 32 32" width="20" height="20" aria-hidden="true">
            <path
              d="M9 19c0-4 3-7 7-7h5l-2-2 1.4-1.4L25 13l-4.6 4.4L19 16l2-2h-5a4 4 0 0 0-4 4v1H9z"
              fill="currentColor"
            />
          </svg>
        </span>
        <span>绿积分 · 回收管理台</span>
      </header>

      <div class="password-card">
        <h1>修改登录密码</h1>
        <p class="sub">
          {{ auth.mustChangePassword ? '当前账号使用临时密码，请先设置新密码后继续使用。' : '修改成功后，所有已签发的登录令牌会立即失效。' }}
        </p>

        <el-alert
          v-if="auth.mustChangePassword"
          title="首次登录或密码被重置，必须修改密码后才能访问其他页面"
          type="warning"
          :closable="false"
          show-icon
          class="tip"
        />

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="submit"
        >
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入当前密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="form.newPassword" type="password" show-password placeholder="8–20 位，需含字母与数字" />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" @keyup.enter="submit" />
          </el-form-item>

          <div class="actions">
            <el-button v-if="!auth.mustChangePassword" @click="router.back()">取消</el-button>
            <el-button type="primary" :loading="loading" native-type="submit" @click.prevent="submit">
              确认修改
            </el-button>
          </div>
        </el-form>

        <p class="foot">密码要求：8–20 位，至少包含一个字母和一个数字，且不能与原密码相同。</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateNew = (rule, value, callback) => {
  if (!value) return callback(new Error('请输入新密码'))
  if (value.length < 8 || value.length > 20) return callback(new Error('密码需为 8–20 位'))
  if (!/[A-Za-z]/.test(value) || !/\d/.test(value)) return callback(new Error('密码需同时包含字母与数字'))
  if (value === form.oldPassword) return callback(new Error('新密码不能与原密码相同'))
  if (form.confirmPassword) formRef.value?.validateField('confirmPassword')
  return callback()
}

const validateConfirm = (rule, value, callback) => {
  if (!value) return callback(new Error('请再次输入新密码'))
  if (value !== form.newPassword) return callback(new Error('两次输入的密码不一致'))
  return callback()
}

const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, validator: validateNew, trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: 'blur' }]
}

async function submit() {
  if (loading.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await auth.changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    ElMessage.success('密码已更新，请使用新密码重新登录')
    auth.clearSession()
    router.replace('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.password-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  background:
    radial-gradient(900px 500px at 50% -10%, rgba(22, 121, 76, 0.1), transparent 60%),
    #f4f7f5;
}

.password-shell {
  width: 100%;
  max-width: 440px;
}

.password-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  color: var(--ink-700);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--brand-600);
  color: #fff;
}

.password-card {
  padding: 32px 34px 28px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: 0 24px 60px -40px rgba(16, 36, 25, 0.4);
}

.password-card h1 {
  font-size: 22px;
  font-weight: 650;
  letter-spacing: -0.01em;
}

.sub {
  margin-top: 6px;
  color: var(--ink-500);
  font-size: 13px;
  line-height: 1.7;
}

.tip {
  margin: 18px 0 6px;
}

.el-form {
  margin-top: 16px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 22px;
}

.actions .el-button {
  min-width: 104px;
}

.foot {
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
  color: var(--ink-400);
  font-size: 11.5px;
  line-height: 1.7;
}
</style>
