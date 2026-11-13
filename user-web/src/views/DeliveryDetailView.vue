<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { userApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { formatTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const record = ref(null)
const error = ref('')

const appealLink = computed(() => {
  if (!record.value) return '/me/appeals'
  return {
    name: 'appeal-new',
    query: {
      targetType: 'DELIVERY',
      targetId: record.value.recordId,
      targetNo: record.value.recordNo,
    },
  }
})

onMounted(async () => {
  try {
    record.value = await userApi.getDeliveryById(route.params.id)
  } catch (err) {
    error.value = err?.message || '投递记录不存在'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <PageHeader title="投递详情" />
    <div class="page-body">
      <el-skeleton v-if="loading" :rows="5" animated />

      <EmptyState v-else-if="error" :text="error">
        <el-button round size="small" @click="router.replace('/me/deliveries')">返回投递记录</el-button>
      </EmptyState>

      <template v-else-if="record">
        <div class="card">
          <div class="detail-top">
            <div>
              <div class="muted small">{{ record.recordNo }}</div>
              <div class="detail-title">{{ record.categoryName }} · {{ record.weight }} kg</div>
            </div>
            <StatusTag :status="record.status" kind="delivery" />
          </div>

          <div class="detail-row">
            <span class="label">获得积分</span>
            <span class="value points">{{ record.points }} 积分</span>
          </div>
          <div class="detail-row">
            <span class="label">投递时间</span>
            <span class="value">{{ formatTime(record.createdAt) }}</span>
          </div>
          <div class="detail-row">
            <span class="label">回收员</span>
            <span class="value">{{ record.recyclerName }}</span>
          </div>
          <div class="detail-row">
            <span class="label">回收站点</span>
            <span class="value">{{ record.siteName || '—' }}</span>
          </div>

          <div v-if="record.status === 'REVOKED'" class="revoke-box">
            <div>该笔流水已被撤销</div>
            <div style="margin-top: 6px">撤销原因：{{ record.revokeReason || '—' }}</div>
            <div style="margin-top: 4px">撤销时间：{{ formatTime(record.revokedAt) }}</div>
            <div style="margin-top: 6px; font-size: 12px">积分已按规则扣回，如对结果有疑问可提交申诉。</div>
          </div>
        </div>

        <div class="card" style="margin-top: 14px">
          <div class="muted small">对这笔投递有疑问？</div>
          <p style="margin: 8px 0 14px; font-size: 14px">可以提交申诉，管理员核实后会给出处理结论。</p>
          <el-button type="primary" plain round style="width: 100%" @click="router.push(appealLink)">
            对该记录申诉
          </el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.detail-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--line);
}

.detail-title {
  margin-top: 6px;
  font-size: 20px;
  font-weight: 700;
}
</style>
