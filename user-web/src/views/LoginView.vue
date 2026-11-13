<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { USE_MOCK } from '@/mock'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ phone: '', password: '' })

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await auth.login(form)
    ElMessage.success('登录成功')
    if (auth.mustChangePassword) {
      router.replace({ name: 'password', query: { forced: '1' } })
      return
    }
    router.replace(String(route.query.redirect || '/'))
  } catch {
    /* 错误提示由请求拦截器统一处理 */
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="brand-mark">绿</div>
    <h1 class="auth-title">绿色校园 · 积分回收</h1>
    <p class="auth-subtitle">投放可回收物，攒积分，换好礼</p>

    <div class="auth-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="submit">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" inputmode="numeric" placeholder="请输入 11 位手机号" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>
      <p v-if="USE_MOCK" class="form-tip" style="margin: 14px 0 6px; text-align: center">
        演示账号：13800001234 / abc12345
      </p>
    </div>

    <p class="auth-footer">
      还没有账号？<router-link to="/register">立即注册</router-link>
    </p>
  </div>
</template>
