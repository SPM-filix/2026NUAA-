<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)
const agreed = ref(false)
const form = reactive({
  phone: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  address: '',
})

const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,20}$/

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 20, message: '昵称最多 20 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!passwordPattern.test(value)) callback(new Error('密码需 8–20 位且同时包含字母与数字'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  address: [{ max: 100, message: '地址备注不能超过 100 个字符', trigger: 'blur' }],
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (!agreed.value) {
    ElMessage.warning('请先阅读并同意用户协议与隐私说明')
    return
  }
  loading.value = true
  try {
    await auth.register({
      phone: form.phone,
      password: form.password,
      nickname: form.nickname,
      address: form.address,
    })
    ElMessage.success('注册成功，已自动登录')
    router.replace('/')
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
    <h1 class="auth-title">注册账号</h1>
    <p class="auth-subtitle">注册即获得 0 积分，投递后由回收员录入积分</p>

    <div class="auth-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" inputmode="numeric" placeholder="用于登录，注册后不可自行修改" clearable />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="20" placeholder="1–20 个字符" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="8–20 位，需含字母与数字" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入密码" />
        </el-form-item>
        <el-form-item label="地址备注（选填）" prop="address">
          <el-input v-model="form.address" maxlength="100" placeholder="如：3 号楼 502" clearable />
        </el-form-item>
        <el-checkbox v-model="agreed" style="margin-bottom: 14px">
          我已阅读并同意《用户协议》与《隐私说明》
        </el-checkbox>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
          注册并登录
        </el-button>
      </el-form>
    </div>

    <p class="auth-footer">
      已有账号？<router-link to="/login">返回登录</router-link>
    </p>
  </div>
</template>
