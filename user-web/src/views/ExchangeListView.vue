<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api'
import { usePagedList } from '@/composables/usePagedList'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { formatTime } from '@/utils/format'

const router = useRouter()
const activeStatus = ref('')
const dialogVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const statusOptions = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '待核销' },
  { value: 'COMPLETED', label: '已核销' },
  { value: 'CANCELLED', label: '已取消' },
]

const { list, total, loading, finished, loadMore, refresh } = usePagedList(({ page, size }) =>
  userApi.getExchanges({ page, size, status: activeStatus.value || undefined }),
)

function changeStatus(value) {
  activeStatus.value = value
  refresh()
}

async function openDetail(item) {
  dialogVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await userApi.getExchange(item.orderId)
  } catch {
    dialogVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => refresh())
</script>

<template>
  <div class="page">
    <PageHeader title="兑换记录" />
    <div class="page-body">
      <div class="chip-row">
        <div
          v-for="item in statusOptions"
          :key="item.value"
          class="chip"
          :class="{ active: activeStatus === item.value }"
          @click="changeStatus(item.value)"
        >
          {{ item.label }}
        </div>
      </div>

      <el-skeleton v-if="loading && list.length === 0" :rows="4" animated />

      <EmptyState v-else-if="!loading && list.length === 0" text="还没有兑换记录，去商城看看吧">
        <el-button type="primary" round size="small" @click="router.push('/mall')">去逛商城</el-button>
      </EmptyState>

      <template v-else>
        <p class="muted small" style="margin: 0 4px 10px">共 {{ total }} 笔订单</p>
        <div v-for="item in list" :key="item.orderId" class="list-card" @click="openDetail(item)">
          <div class="order-top">
            <img :src="item.productCoverUrl" :alt="item.productName" />
            <div class="order-info">
              <div class="list-top">
                <span class="list-title">{{ item.productName }}</span>
                <StatusTag :status="item.status" kind="order" />
              </div>
              <div class="list-meta">
                <span>数量 {{ item.quantity }}</span>
                <span>{{ item.pointsCost }} 积分</span>
              </div>
              <div class="list-foot">
                <span>{{ formatTime(item.createdAt) }}</span>
                <span>{{ item.orderNo }}</span>
              </div>
            </div>
          </div>

          <div v-if="item.status === 'PENDING'" class="pending-code">
            <span class="muted small">核销码</span>
            <span class="code">{{ item.redeemCode }}</span>
            <span class="muted small">点击卡片查看大图</span>
          </div>
        </div>

        <div class="load-more">
          <el-button v-if="!finished" :loading="loading" round @click="loadMore">加载更多</el-button>
          <span v-else class="muted small">没有更多了</span>
        </div>
      </template>
    </div>

    <el-dialog v-model="dialogVisible" title="订单详情" width="88%" align-center>
      <el-skeleton v-if="detailLoading" :rows="4" animated />
      <template v-else-if="detail">
        <div class="dialog-product">
          <img :src="detail.productCoverUrl" :alt="detail.productName" />
          <div>
            <div style="font-weight: 600">{{ detail.productName }}</div>
            <div class="muted small">数量 {{ detail.quantity }} · {{ detail.pointsCost }} 积分</div>
          </div>
        </div>

        <div class="redeem-code" v-if="detail.status === 'PENDING'">{{ detail.redeemCode }}</div>
        <p v-if="detail.status === 'PENDING'" class="muted small" style="text-align: center; margin-top: 0">
          有效期至 {{ formatTime(detail.redeemCodeExpireAt) }}
        </p>

        <div class="detail-row">
          <span class="label">订单号</span>
          <span class="value">{{ detail.orderNo }}</span>
        </div>
        <div class="detail-row">
          <span class="label">状态</span>
          <span class="value"><StatusTag :status="detail.status" kind="order" /></span>
        </div>
        <div class="detail-row">
          <span class="label">下单时间</span>
          <span class="value">{{ formatTime(detail.createdAt) }}</span>
        </div>
        <template v-if="detail.status === 'COMPLETED'">
          <div class="detail-row">
            <span class="label">核销时间</span>
            <span class="value">{{ formatTime(detail.verifiedAt) }}</span>
          </div>
          <div class="detail-row">
            <span class="label">核销站点</span>
            <span class="value">{{ detail.siteName || '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">核销人</span>
            <span class="value">{{ detail.verifierName || '—' }}</span>
          </div>
        </template>
        <div v-if="detail.status === 'CANCELLED'" class="revoke-box">
          取消原因：{{ detail.cancelReason || '—' }}
          <div style="margin-top: 4px; font-size: 12px">订单取消后积分已按规则退回。</div>
        </div>

        <el-button
          plain
          round
          style="width: 100%; margin-top: 16px"
          @click="
            router.push({
              name: 'appeal-new',
              query: { targetType: 'EXCHANGE', targetId: detail.orderId, targetNo: detail.orderNo },
            })
          "
        >
          对该订单申诉
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.order-top {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.order-top img {
  flex: none;
  width: 72px;
  height: 72px;
  object-fit: cover;
  background: #eef3f0;
  border-radius: 10px;
}

.order-info {
  flex: 1;
  min-width: 0;
}

.pending-code {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-top: 12px;
  background: var(--brand-soft);
  border-radius: 10px;
}

.pending-code .code {
  flex: 1;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--brand-dark);
}
</style>
