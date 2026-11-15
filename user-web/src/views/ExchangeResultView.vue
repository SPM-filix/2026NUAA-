<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { userApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { formatTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const order = ref(null)
const balance = ref(null)
const error = ref('')

onMounted(async () => {
  try {
    const [detail, points] = await Promise.all([
      userApi.getExchange(route.params.orderId),
      userApi.getPoints().catch(() => null),
    ])
    order.value = detail
    balance.value = points?.balance ?? null
  } catch (err) {
    error.value = err?.message || '订单不存在'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <PageHeader title="兑换结果" :back="false" />
    <div class="page-body">
      <el-skeleton v-if="loading" :rows="5" animated />

      <EmptyState v-else-if="error" :text="error">
        <el-button round size="small" @click="router.replace('/me/exchanges')">查看兑换记录</el-button>
      </EmptyState>

      <template v-else-if="order">
        <div class="result-head">
          <el-icon class="result-icon"><CircleCheckFilled /></el-icon>
          <div class="result-title">兑换成功</div>
          <p class="muted small">请到回收站点向回收员出示核销码领取礼品</p>
        </div>

        <div class="card">
          <div class="redeem-code">{{ order.redeemCode }}</div>
          <p class="muted small" style="margin: -4px 0 12px; text-align: center">
            核销码有效期至 {{ formatTime(order.redeemCodeExpireAt) }}
          </p>
          <div class="detail-row">
            <span class="label">订单号</span>
            <span class="value">{{ order.orderNo }}</span>
          </div>
          <div class="detail-row">
            <span class="label">商品</span>
            <span class="value">{{ order.productName }} × {{ order.quantity }}</span>
          </div>
          <div class="detail-row">
            <span class="label">消耗积分</span>
            <span class="value">{{ order.pointsCost }} 积分</span>
          </div>
          <div v-if="balance !== null" class="detail-row">
            <span class="label">剩余积分</span>
            <span class="value points">{{ balance }} 积分</span>
          </div>
        </div>

        <div style="margin-top: 18px; display: flex; flex-direction: column; gap: 10px">
          <el-button type="primary" size="large" round @click="router.replace('/me/exchanges')">
            查看兑换记录
          </el-button>
          <el-button size="large" round @click="router.replace('/mall')">继续逛商城</el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.result-head {
  padding: 24px 0 20px;
  text-align: center;
}

.result-icon {
  font-size: 58px;
  color: var(--brand);
}

.result-title {
  margin: 8px 0 6px;
  font-size: 22px;
  font-weight: 700;
}
</style>
