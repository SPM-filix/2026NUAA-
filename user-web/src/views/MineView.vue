<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const menus = [
  { label: '投递记录', icon: 'Document', path: '/me/deliveries' },
  { label: '积分明细', icon: 'Wallet', path: '/me/points' },
  { label: '兑换记录', icon: 'Ticket', path: '/me/exchanges' },
  { label: '我的申诉', icon: 'ChatDotRound', path: '/me/appeals' },
  { label: '个人资料', icon: 'User', path: '/me/profile' },
  { label: '修改密码', icon: 'Lock', path: '/me/password' },
]

async function logout() {
  try {
    await ElMessageBox.confirm('退出后需要重新登录才能查看积分和兑换记录。', '确认退出登录', {
      confirmButtonText: '退出登录',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await auth.logout()
  router.replace('/login')
}

onMounted(() => {
  auth.fetchMe().catch(() => {})
})
</script>

<template>
  <div class="page">
    <div class="page-body">
      <div class="profile-card">
        <el-avatar :size="56" :src="auth.user?.avatarUrl || ''">
          {{ (auth.user?.nickname || '绿').slice(0, 1) }}
        </el-avatar>
        <div class="profile-info">
          <div class="profile-name">{{ auth.user?.nickname || '未登录' }}</div>
          <div class="profile-code">用户编号 {{ auth.user?.userCode || '—' }}</div>
        </div>
        <div class="profile-points">
          <b>{{ auth.user?.pointsBalance ?? 0 }}</b>
          <span>积分</span>
        </div>
      </div>

      <div class="menu-list" style="margin-top: 16px">
        <div v-for="item in menus" :key="item.path" class="menu-item" @click="router.push(item.path)">
          <el-icon><component :is="item.icon" /></el-icon>
          <span class="menu-text">{{ item.label }}</span>
          <el-icon class="arrow"><ArrowRight /></el-icon>
        </div>
      </div>

      <div class="menu-list" style="margin-top: 14px">
        <div class="menu-item" @click="logout">
          <el-icon style="color: var(--danger)"><SwitchButton /></el-icon>
          <span class="menu-text" style="color: var(--danger)">退出登录</span>
        </div>
      </div>

      <p class="muted small" style="margin-top: 18px; text-align: center">
        积分只能由回收员录入或系统发放，用户端不可修改
      </p>
    </div>
  </div>
</template>

<style scoped>
.profile-card {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 20px 18px;
  background: #fff;
  border-radius: var(--radius);
  box-shadow: 0 2px 10px rgba(31, 41, 51, 0.04);
}

.profile-info {
  flex: 1;
}

.profile-name {
  font-size: 18px;
  font-weight: 700;
}

.profile-code {
  margin-top: 4px;
  font-size: 12px;
  color: var(--muted);
}

.profile-points {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  color: var(--brand-dark);
}

.profile-points b {
  font-size: 22px;
}

.profile-points span {
  font-size: 12px;
  color: var(--muted);
}
</style>
