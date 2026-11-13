<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { mallApi, userApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const submitting = ref(false)
const product = ref(null)
const balance = ref(0)
const quantity = ref(1)
const error = ref('')
let idempotencyKey = ''

const maxQuantity = computed(() => Math.min(product.value?.stock || 0, 5))
const totalCost = computed(() => (product.value?.pointsPrice || 0) * quantity.value)
const blockedReason = computed(() => {
  if (!product.value) return ''
  if (product.value.status !== 'ON') return '已下架'
  if (product.value.stock <= 0) return '已兑完'
  if (balance.value < totalCost.value) return `还差 ${totalCost.value - balance.value} 积分`
  return ''
})

function newKey() {
  if (window.crypto?.randomUUID) return window.crypto.randomUUID()
  return `key-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

onMounted(async () => {
  try {
    const [detail, points] = await Promise.all([
      mallApi.getProduct(route.params.id),
      userApi.getPoints(),
    ])
    product.value = detail
    balance.value = points.balance
    quantity.value = Math.min(1, Math.max(1, detail.stock))
  } catch (err) {
    error.value = err?.message || '商品不存在'
  } finally {
    loading.value = false
  }
})

async function submit() {
  if (blockedReason.value || submitting.value) return
  try {
    await ElMessageBox.confirm(
      `将使用 ${totalCost.value} 积分兑换「${product.value.name}」× ${quantity.value}，兑换后积分不可退回。`,
      '确认兑换',
      { confirmButtonText: '确认兑换', cancelButtonText: '再想想', type: 'warning' },
    )
  } catch {
    return
  }

  if (!idempotencyKey) idempotencyKey = newKey()
  submitting.value = true
  try {
    const data = await mallApi.exchange({
      productId: product.value.productId,
      quantity: quantity.value,
      idempotencyKey,
    })
    ElMessage.success('兑换成功')
    router.replace(`/mall/result/${data.orderId}`)
  } catch {
    idempotencyKey = ''
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="商品详情" />
    <div class="page-body">
      <el-skeleton v-if="loading" :rows="6" animated />

      <EmptyState v-else-if="error" :text="error">
        <el-button round size="small" @click="router.replace('/mall')">返回商城</el-button>
      </EmptyState>

      <template v-else-if="product">
        <div class="cover-box">
          <img :src="product.coverUrl" :alt="product.name" />
          <div v-if="product.stock <= 0" class="sold-out">已兑完</div>
        </div>

        <div class="card" style="margin-top: 14px">
          <div class="detail-top">
            <div class="detail-title">{{ product.name }}</div>
            <span class="points price">{{ product.pointsPrice }} 积分</span>
          </div>
          <div class="list-meta">
            <span>库存 {{ product.stock }}</span>
            <span v-if="product.spec">{{ product.spec }}</span>
            <span>已兑 {{ product.salesCount }} 件</span>
          </div>

          <div v-if="product.description" style="margin-top: 14px">
            <div class="block-title">商品介绍</div>
            <p class="block-text">{{ product.description }}</p>
          </div>

          <div v-if="product.exchangeNotice" style="margin-top: 14px">
            <div class="block-title">兑换须知</div>
            <p class="block-text">{{ product.exchangeNotice }}</p>
          </div>
        </div>

        <div class="card" style="margin-top: 14px">
          <div class="detail-row">
            <span class="label">当前积分</span>
            <span class="value">{{ balance }} 积分</span>
          </div>
          <div class="detail-row">
            <span class="label">兑换数量</span>
            <span class="value">
              <el-input-number v-model="quantity" :min="1" :max="Math.max(maxQuantity, 1)" :disabled="maxQuantity < 1" size="small" />
            </span>
          </div>
          <div class="detail-row">
            <span class="label">合计消耗</span>
            <span class="value points">{{ totalCost }} 积分</span>
          </div>
        </div>

        <div style="margin-top: 18px">
          <el-button
            type="primary"
            size="large"
            round
            style="width: 100%"
            :loading="submitting"
            :disabled="Boolean(blockedReason)"
            @click="submit"
          >
            {{ blockedReason || '立即兑换' }}
          </el-button>
          <p class="muted small" style="margin-top: 10px; text-align: center">
            兑换成功后生成唯一核销码，请到回收站点出示
          </p>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.cover-box {
  position: relative;
  overflow: hidden;
  width: 100%;
  aspect-ratio: 1;
  background: #eef3f0;
  border-radius: var(--radius);
}

.cover-box img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-box .sold-out {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  background: rgba(31, 41, 51, 0.45);
}

.detail-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.detail-title {
  font-size: 20px;
  font-weight: 700;
}

.price {
  flex: none;
  font-size: 18px;
}

.block-title {
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
}

.block-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #3e4c59;
  white-space: pre-wrap;
}
</style>
