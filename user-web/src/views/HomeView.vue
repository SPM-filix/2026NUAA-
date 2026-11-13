<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { formatTime } from '@/utils/format'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(true)
const summary = ref(null)
const recent = ref([])

const quickLinks = [
  { label: '投递记录', icon: 'Document', path: '/me/deliveries' },
  { label: '积分明细', icon: 'Wallet', path: '/me/points' },
  { label: '兑换记录', icon: 'Ticket', path: '/me/exchanges' },
  { label: '公告科普', icon: 'Bell', path: '/announcements' },
]

onMounted(async () => {
  try {
    const [points, deliveries] = await Promise.all([
      userApi.getPoints(),
      userApi.getDeliveries({ page: 1, size: 3 }),
      auth.fetchMe(),
    ])
    summary.value = points
    recent.value = deliveries?.list || []
  } catch {
    /* 错误提示由请求拦截器统一处理 */
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <div class="page-body">
      <div class="home-head">
        <div>
          <p class="hello">你好，{{ auth.user?.nickname || '同学' }}</p>
          <p class="muted small">用户编号 {{ auth.user?.userCode || '—' }}</p>
        </div>
        <el-avatar :size="44" :src="auth.user?.avatarUrl || ''">
          {{ (auth.user?.nickname || '绿').slice(0, 1) }}
        </el-avatar>
      </div>

      <el-skeleton v-if="loading" :rows="4" animated style="margin-top: 16px" />

      <template v-else>
        <div class="balance-card" role="button" tabindex="0" @click="router.push('/me/points')">
          <div class="balance-label">当前积分</div>
          <div class="balance-value">{{ summary?.balance ?? auth.balance }}</div>
          <div class="balance-row">
            <span>累计收入<b>{{ summary?.totalIncome ?? 0 }}</b></span>
            <span>累计支出<b>{{ summary?.totalExpense ?? 0 }}</b></span>
            <span>撤销扣回<b>{{ summary?.totalRevoked ?? 0 }}</b></span>
          </div>
        </div>

        <div class="qr-entry card" role="button" tabindex="0" @click="router.push('/me/qrcode')">
          <div class="qr-icon"><el-icon><FullScreen /></el-icon></div>
          <div class="qr-text">
            <div class="qr-title">向回收员出示身份码</div>
            <div class="muted small">扫码或报 6 位用户编号即可登记投递</div>
          </div>
          <el-icon class="arrow"><ArrowRight /></el-icon>
        </div>

        <div class="quick-grid">
          <div v-for="item in quickLinks" :key="item.path" class="quick-item" @click="router.push(item.path)">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </div>
        </div>

        <div class="section-title">
          <span>最近投递</span>
          <router-link class="link" to="/me/deliveries">查看全部</router-link>
        </div>

        <EmptyState v-if="recent.length === 0" text="还没有投递记录，去投放点出示二维码吧">
          <el-button type="primary" round size="small" @click="router.push('/me/qrcode')">出示身份码</el-button>
        </EmptyState>

        <div
          v-for="item in recent"
          :key="item.recordId"
          class="list-card"
          :class="{ revoked: item.status === 'REVOKED' }"
          @click="router.push(`/me/deliveries/${item.recordId}`)"
        >
          <div class="list-top">
            <span class="list-title">{{ item.categoryName }} · {{ item.weight }} kg</span>
            <span class="points">{{ item.points }} 积分</span>
          </div>
          <div class="list-foot">
            <span>{{ formatTime(item.createdAt) }} · {{ item.recyclerName }}</span>
            <StatusTag :status="item.status" kind="delivery" />
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.home-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 4px 18px;
}

.hello {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 700;
}

.qr-entry {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 14px;
  cursor: pointer;
}

.qr-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  font-size: 22px;
  color: var(--brand);
  background: var(--brand-soft);
  border-radius: 12px;
}

.qr-text {
  flex: 1;
}

.qr-title {
  font-size: 15px;
  font-weight: 600;
}

.qr-entry .arrow {
  color: #c2cbc5;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 14px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
  padding: 14px 0;
  font-size: 12px;
  color: var(--text);
  cursor: pointer;
  background: #fff;
  border-radius: 14px;
}

.quick-item .el-icon {
  font-size: 21px;
  color: var(--brand);
}
</style>
