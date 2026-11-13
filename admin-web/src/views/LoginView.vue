<template>
  <div class="login-page">
    <section class="hero">
      <div class="hero__rings" aria-hidden="true">
        <span class="ring ring--1" />
        <span class="ring ring--2" />
        <span class="ring ring--3" />
      </div>

      <div class="hero__top">
        <span class="brand">
          <span class="brand__mark">
            <svg viewBox="0 0 32 32" width="20" height="20" aria-hidden="true">
              <path
                d="M9 19c0-4 3-7 7-7h5l-2-2 1.4-1.4L25 13l-4.6 4.4L19 16l2-2h-5a4 4 0 0 0-4 4v1H9z"
                fill="currentColor"
              />
            </svg>
          </span>
          <span>绿积分</span>
        </span>
        <span class="hero__tag">NUAA · 2026 软件工程</span>
      </div>

      <div class="hero__copy">
        <p class="eyebrow">CAMPUS RECYCLING PLATFORM</p>
        <h1>让每一次投递<br />都有回响。</h1>
        <p class="lead">
          回收员完成称重登记与兑换核销，管理员守护积分规则与业务数据。<br />
          同一套管理台，两种职责，一条完整的回收链路。
        </p>
      </div>

      <div class="hero__facts">
        <div class="fact">
          <strong>3 类角色</strong>
          <span>用户 / 回收员 / 管理员</span>
        </div>
        <div class="fact">
          <strong>5 分钟</strong>
          <span>回收员自助撤销窗口</span>
        </div>
        <div class="fact">
          <strong>全链路</strong>
          <span>投递 → 积分 → 兑换 → 核销</span>
        </div>
      </div>

      <p class="hero__foot">校园垃圾回收积分系统 · 管理端</p>
    </section>

    <section class="login-area">
      <div class="login-card">
        <header class="login-card__head">
          <h2>登录管理台</h2>
          <p>请选择身份，并使用对应账号登录</p>
        </header>

        <div class="role-switch" role="tablist">
          <button
            type="button"
            :class="{ active: role === 'RECYCLER' }"
            @click="switchRole('RECYCLER')"
          >
            <el-icon><Postcard /></el-icon>
            回收员
          </button>
          <button
            type="button"
            :class="{ active: role === 'ADMIN' }"
            @click="switchRole('ADMIN')"
          >
            <el-icon><Setting /></el-icon>
            管理员
          </button>
        </div>

        <transition name="fade-slide">
          <el-alert
            v-if="errorMessage"
            :title="errorMessage"
            type="error"
            :closable="false"
            show-icon
            class="login-error"
          />
        </transition>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          class="login-form"
          @submit.prevent="submit"
        >
          <el-form-item :label="role === 'ADMIN' ? '管理员账号' : '手机号'" prop="account">
            <el-input
              v-model="form.account"
              :placeholder="role === 'ADMIN' ? '请输入管理员账号' : '请输入 11 位手机号'"
              :prefix-icon="role === 'ADMIN' ? User : Phone"
              autocomplete="username"
              clearable
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入登录密码"
              :prefix-icon="Lock"
              autocomplete="current-password"
              show-password
              @keyup.enter="submit"
            />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="submit"
            :loading="loading"
            native-type="submit"
            @click.prevent="submit"
          >
            {{ loading ? '正在登录…' : '登 录' }}
          </el-button>
        </el-form>

        <div v-if="USE_MOCK" class="demo">
          <button type="button" class="demo__toggle" @click="showDemo = !showDemo">
            <span>阶段②演示账号</span>
            <el-icon><ArrowDown v-if="!showDemo" /><ArrowUp v-else /></el-icon>
          </button>
          <div v-if="showDemo" class="demo__list">
            <button type="button" class="demo__item" @click="fillAccount('ADMIN')">
              <span class="demo__role">管理员</span>
              <span class="demo__cred mono">admin / admin123</span>
              <el-icon><Right /></el-icon>
            </button>
            <button type="button" class="demo__item" @click="fillAccount('RECYCLER')">
              <span class="demo__role">回收员</span>
              <span class="demo__cred mono">13900001001 / recycler123</span>
              <el-icon><Right /></el-icon>
            </button>
            <button type="button" class="demo__item" @click="fillAccount('PENDING')">
              <span class="demo__role">待审核</span>
              <span class="demo__cred mono">13900001003 / recycler123</span>
              <el-icon><Right /></el-icon>
            </button>
            <button type="button" class="demo__item" @click="fillAccount('REJECTED')">
              <span class="demo__role">已驳回</span>
              <span class="demo__cred mono">13900001004 / recycler123</span>
              <el-icon><Right /></el-icon>
            </button>
          </div>
        </div>

        <p class="login-card__foot">2026 NUAA 软件工程课程 · 校园垃圾回收积分系统</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Phone, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { USE_MOCK } from '@/api/request'

const router = useRouter()
const auth = useAuthStore()

const role = ref('RECYCLER')
const loading = ref(false)
const errorMessage = ref('')
const showDemo = ref(false)
const formRef = ref(null)

const form = reactive({ account: '', password: '' })

const rules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const DEMO_ACCOUNTS = {
  ADMIN: { role: 'ADMIN', account: 'admin', password: 'admin123' },
  RECYCLER: { role: 'RECYCLER', account: '13900001001', password: 'recycler123' },
  PENDING: { role: 'RECYCLER', account: '13900001003', password: 'recycler123' },
  REJECTED: { role: 'RECYCLER', account: '13900001004', password: 'recycler123' }
}

function switchRole(nextRole) {
  role.value = nextRole
  errorMessage.value = ''
  form.password = ''
}

function fillAccount(key) {
  const demo = DEMO_ACCOUNTS[key]
  if (!demo) return
  role.value = demo.role
  form.account = demo.account
  form.password = demo.password
  errorMessage.value = ''
}

async function submit() {
  if (loading.value) return
  errorMessage.value = ''
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    if (role.value === 'ADMIN') {
      await auth.loginAsAdmin({ username: form.account.trim(), password: form.password })
    } else {
      await auth.loginAsRecycler({ phone: form.account.trim(), password: form.password })
    }
    ElMessage.success(`欢迎回来，${auth.displayName}`)
    router.replace(auth.homePath)
  } catch (error) {
    errorMessage.value =
      error?.code === 2001 || error?.code === 2002 || error?.code === 2004
        ? error.message
        : '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(420px, 0.92fr);
  min-height: 100vh;
  background: #f7faf8;
}

/* ---------------- 左侧品牌区 ---------------- */
.hero {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 40px clamp(32px, 5vw, 76px) 34px;
  background:
    radial-gradient(1100px 520px at 10% 0%, rgba(63, 207, 142, 0.2), transparent 60%),
    linear-gradient(158deg, #0d3322 0%, #0a2418 55%, #081d13 100%);
  color: #fff;
  overflow: hidden;
}

.hero__rings {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.ring {
  position: absolute;
  border: 1px solid rgba(255, 255, 255, 0.075);
  border-radius: 50%;
}

.ring--1 {
  width: 620px;
  height: 620px;
  right: -240px;
  bottom: -220px;
}

.ring--2 {
  width: 440px;
  height: 440px;
  right: -150px;
  bottom: -130px;
  border-color: rgba(63, 207, 142, 0.16);
}

.ring--3 {
  width: 260px;
  height: 260px;
  right: -60px;
  bottom: -40px;
  border-color: rgba(63, 207, 142, 0.24);
}

.hero__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  position: relative;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.brand__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3fcf8e, #16794c);
  color: #fff;
}

.hero__tag {
  padding: 4px 12px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 11.5px;
  letter-spacing: 0.06em;
}

.hero__copy {
  position: relative;
  max-width: 560px;
  margin: 36px 0;
}

.eyebrow {
  margin-bottom: 18px;
  color: #5cdd9e;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.24em;
}

.hero h1 {
  font-size: clamp(38px, 4.2vw, 56px);
  font-weight: 650;
  line-height: 1.18;
  letter-spacing: -0.02em;
}

.lead {
  margin-top: 22px;
  color: rgba(226, 240, 232, 0.66);
  font-size: 14px;
  line-height: 1.9;
}

.hero__facts {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  max-width: 620px;
  margin-top: auto;
}

.fact {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border: 1px solid rgba(255, 255, 255, 0.09);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.035);
}

.fact strong {
  font-size: 15px;
  font-weight: 600;
}

.fact span {
  color: rgba(226, 240, 232, 0.5);
  font-size: 11.5px;
}

.hero__foot {
  position: relative;
  margin-top: 26px;
  color: rgba(255, 255, 255, 0.28);
  font-size: 11px;
  letter-spacing: 0.08em;
}

/* ---------------- 右侧登录区 ---------------- */
.login-area {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 28px;
  background:
    radial-gradient(800px 400px at 100% 0%, rgba(22, 121, 76, 0.06), transparent 60%),
    #f7faf8;
}

.login-card {
  width: 100%;
  max-width: 400px;
  animation: card-in 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.login-card__head h2 {
  font-size: 26px;
  font-weight: 650;
  letter-spacing: -0.02em;
}

.login-card__head p {
  margin-top: 6px;
  color: var(--ink-500);
  font-size: 13px;
}

.role-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  margin: 26px 0 22px;
  padding: 5px;
  background: #edf2ef;
  border-radius: 12px;
}

.role-switch button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 42px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: var(--ink-500);
  font-family: inherit;
  font-size: 13.5px;
  font-weight: 550;
  cursor: pointer;
  transition: all 0.18s ease;
}

.role-switch button:hover {
  color: var(--ink-900);
}

.role-switch button.active {
  background: #fff;
  color: var(--brand-700);
  box-shadow: 0 2px 8px rgba(16, 36, 25, 0.08);
}

.login-error {
  margin-bottom: 16px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-form :deep(.el-form-item__label) {
  font-size: 12.5px;
}

.submit {
  width: 100%;
  height: 46px;
  margin-top: 4px;
  border-radius: 10px;
  font-size: 14.5px;
  letter-spacing: 0.04em;
}

.demo {
  margin-top: 26px;
  border-top: 1px solid var(--line);
  padding-top: 14px;
}

.demo__toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  border: 0;
  background: transparent;
  color: var(--ink-500);
  font-family: inherit;
  font-size: 12.5px;
  cursor: pointer;
  padding: 2px 0;
}

.demo__toggle:hover {
  color: var(--brand-600);
}

.demo__list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 10px;
}

.demo__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border: 1px solid var(--line);
  border-radius: 9px;
  background: #fff;
  font-family: inherit;
  font-size: 12px;
  color: var(--ink-700);
  cursor: pointer;
  transition: border-color 0.16s ease, background 0.16s ease;
}

.demo__item:hover {
  border-color: var(--brand-500);
  background: var(--brand-50);
}

.demo__role {
  flex: none;
  color: var(--brand-700);
  font-weight: 600;
}

.demo__cred {
  color: var(--ink-500);
  font-size: 11px;
}

.demo__item .el-icon {
  margin-left: auto;
  color: var(--ink-400);
}

.login-card__foot {
  margin-top: 28px;
  color: var(--ink-400);
  font-size: 11px;
  text-align: center;
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .hero {
    display: none;
  }

  .login-area {
    min-height: 100vh;
    padding: 24px 20px;
  }
}
</style>
