<template>
  <div class="forbidden">
    <div class="forbidden__card">
      <span class="forbidden__icon">
        <el-icon><Lock /></el-icon>
      </span>
      <h1>当前角色无权访问</h1>
      <p>
        你正在以「{{ auth.roleLabel }}」身份访问该页面。<br />
        前端隐藏入口不代表权限，越权请求会被后端 403 拦截。
      </p>

      <div class="role-row">
        <span class="role-chip" :class="{ active: auth.isRecycler }">
          <el-icon><Postcard /></el-icon> 回收员
        </span>
        <span class="role-chip" :class="{ active: auth.isAdmin }">
          <el-icon><Setting /></el-icon> 管理员
        </span>
      </div>

      <div class="forbidden__actions">
        <el-button type="primary" @click="router.replace(auth.homePath)">返回我的首页</el-button>
        <el-button @click="logout">切换账号</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

async function logout() {
  await auth.logout()
  router.replace('/login')
}
</script>

<style scoped>
.forbidden {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(800px 420px at 50% 0%, rgba(22, 121, 76, 0.08), transparent 60%),
    #f4f7f5;
}

.forbidden__card {
  width: 100%;
  max-width: 440px;
  padding: 40px 36px 32px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: 0 24px 60px -42px rgba(16, 36, 25, 0.4);
  text-align: center;
}

.forbidden__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: var(--amber-50);
  color: var(--amber-700);
  font-size: 26px;
}

.forbidden__card h1 {
  margin-top: 20px;
  font-size: 21px;
  font-weight: 650;
}

.forbidden__card p {
  margin-top: 10px;
  color: var(--ink-500);
  font-size: 13px;
  line-height: 1.8;
}

.role-row {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin: 22px 0 26px;
}

.role-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: 1px solid var(--line);
  border-radius: 999px;
  color: var(--ink-400);
  font-size: 12.5px;
}

.role-chip.active {
  border-color: var(--brand-500);
  background: var(--brand-50);
  color: var(--brand-700);
  font-weight: 600;
}

.forbidden__actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}
</style>
