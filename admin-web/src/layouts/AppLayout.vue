<template>
  <div class="shell" :class="{ 'is-collapsed': collapsed }">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand__mark">
          <svg viewBox="0 0 32 32" width="22" height="22" aria-hidden="true">
            <path
              d="M9 19c0-4 3-7 7-7h5l-2-2 1.4-1.4L25 13l-4.6 4.4L19 16l2-2h-5a4 4 0 0 0-4 4v1H9z"
              fill="currentColor"
            />
          </svg>
        </span>
        <span v-show="!collapsed" class="brand__text">
          <strong>绿积分</strong>
          <small>回收管理台</small>
        </span>
      </div>

      <el-scrollbar class="nav">
        <el-menu
          :default-active="route.path"
          :collapse="collapsed"
          :collapse-transition="false"
          router
          class="nav-menu"
        >
          <template v-for="group in menus" :key="group.group">
            <p v-show="!collapsed" class="nav-group">{{ group.group }}</p>
            <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>

      <div class="sidebar__foot">
        <template v-if="!collapsed">
          <span class="pulse" />
          <span>{{ auth.roleLabel }}视图</span>
          <span v-if="USE_MOCK" class="foot-tag">Mock</span>
        </template>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="topbar__left">
          <el-button text circle class="collapse-btn" @click="collapsed = !collapsed">
            <el-icon><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>管理台</el-breadcrumb-item>
            <el-breadcrumb-item v-if="menuContext.group">{{ menuContext.group }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ menuContext.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="topbar__right">
          <span v-if="USE_MOCK" class="mock-pill"><i />Mock 数据</span>
          <span class="topbar__date">{{ today }}</span>
          <el-divider direction="vertical" />
          <el-dropdown trigger="click" @command="handleCommand">
            <button class="user" type="button">
              <span class="avatar-initial">{{ initial }}</span>
              <span class="user__meta">
                <strong>{{ auth.displayName }}</strong>
                <small>
                  {{ auth.roleLabel }}
                  <template v-if="auth.siteName"> · {{ auth.siteName }}</template>
                </small>
              </span>
              <el-icon class="user__caret"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { findMenuContext, menuForRole } from '@/router/menu'
import { USE_MOCK } from '@/api/request'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(window.innerWidth < 1180)

const menus = computed(() => menuForRole(auth.role))
const menuContext = computed(() => findMenuContext(auth.role, route.path))
const initial = computed(() => String(auth.displayName || '?').slice(0, 1))
const today = computed(() =>
  new Intl.DateTimeFormat('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'short'
  }).format(new Date())
)

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'password') {
    router.push('/change-password')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('退出后当前令牌立即失效，需要重新登录。', '退出登录', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }
    await auth.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.shell {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--bg);
}

/* ---------------- 侧边栏 ---------------- */
.sidebar {
  display: flex;
  flex-direction: column;
  width: 236px;
  flex: none;
  background: linear-gradient(180deg, #0e3222 0%, #0a2418 100%);
  color: #c7d8ce;
  transition: width 0.22s ease;
  position: relative;
  z-index: 2;
}

.is-collapsed .sidebar {
  width: 78px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  height: 68px;
  padding: 0 20px;
  flex: none;
}

.is-collapsed .brand {
  justify-content: center;
  padding: 0;
}

.brand__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 11px;
  background: linear-gradient(135deg, #2fbd7c, #16794c);
  color: #fff;
  box-shadow: 0 6px 16px -8px rgba(47, 189, 124, 0.9);
  flex: none;
}

.brand__text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand__text strong {
  color: #fff;
  font-size: 15.5px;
  letter-spacing: 0.02em;
}

.brand__text small {
  color: rgba(199, 216, 206, 0.55);
  font-size: 11px;
  letter-spacing: 0.08em;
}

.nav {
  flex: 1;
  min-height: 0;
}

.nav-menu {
  border-right: 0;
  background: transparent;
  padding: 6px 0 16px;
}

.nav-menu:not(.el-menu--collapse) {
  width: 100%;
}

.nav-group {
  margin: 18px 24px 6px;
  color: rgba(255, 255, 255, 0.32);
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.12em;
}

.nav-menu :deep(.el-menu-item) {
  height: 42px;
  line-height: 42px;
  margin: 2px 12px;
  border-radius: 9px;
  color: #aec3b7;
  font-size: 13.5px;
  transition: background 0.16s ease, color 0.16s ease;
}

.nav-menu :deep(.el-menu-item .el-icon) {
  font-size: 17px;
  margin-right: 10px;
}

.nav-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.nav-menu :deep(.el-menu-item.is-active) {
  background: rgba(63, 207, 142, 0.16);
  color: #fff;
  box-shadow: inset 3px 0 0 #3fcf8e;
}

.nav-menu :deep(.el-menu-item.is-active .el-icon) {
  color: #5cdd9e;
}

.nav-menu.el-menu--collapse :deep(.el-menu-item) {
  margin: 2px 9px;
  justify-content: center;
}

.nav-menu.el-menu--collapse :deep(.el-menu-item .el-icon) {
  margin-right: 0;
}

.sidebar__foot {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 52px;
  padding: 0 22px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  color: rgba(199, 216, 206, 0.62);
  font-size: 12px;
  flex: none;
}

.pulse {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #3fcf8e;
  box-shadow: 0 0 0 3px rgba(63, 207, 142, 0.16);
}

.foot-tag {
  margin-left: auto;
  padding: 1px 7px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.55);
  font-size: 10px;
  letter-spacing: 0.04em;
}

/* ---------------- 主区域 ---------------- */
.main {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 68px;
  flex: none;
  padding: 0 26px 0 16px;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line);
}

.topbar__left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.collapse-btn {
  color: var(--ink-500);
}

.topbar__left :deep(.el-breadcrumb) {
  font-size: 13px;
}

.topbar__left :deep(.el-breadcrumb__inner) {
  color: var(--ink-500);
  font-weight: 500;
}

.topbar__left :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--ink-900);
  font-weight: 600;
}

.topbar__right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.mock-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--amber-50);
  color: var(--amber-700);
  font-size: 11.5px;
  font-weight: 600;
}

.mock-pill i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #d99a1f;
}

.topbar__date {
  color: var(--ink-500);
  font-size: 12.5px;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 0;
  background: transparent;
  padding: 4px 6px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.16s ease;
}

.user:hover {
  background: var(--brand-50);
}

.user__meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.25;
}

.user__meta strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-900);
}

.user__meta small {
  color: var(--ink-500);
  font-size: 11px;
}

.user__caret {
  color: var(--ink-400);
  font-size: 12px;
}

.content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 26px 30px 48px;
}

@media (max-width: 1180px) {
  .content {
    padding: 20px 18px 40px;
  }

  .topbar__date {
    display: none;
  }
}
</style>
