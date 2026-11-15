<script setup>
import { onMounted, ref } from 'vue'
import { userApi } from '@/api'
import { usePagedList } from '@/composables/usePagedList'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { bizTypeText, formatTime, pointsTypeText, signedPoints } from '@/utils/format'

const summary = ref(null)
const activeType = ref('')

const typeOptions = [
  { value: '', label: '全部' },
  { value: 'INCOME', label: '收入' },
  { value: 'EXPENSE', label: '支出' },
  { value: 'REVOKE', label: '撤销' },
  { value: 'ADJUST', label: '调整' },
  { value: 'REFUND', label: '退回' },
]

const { list, total, loading, finished, loadMore, refresh } = usePagedList(({ page, size }) =>
  userApi.getPointsLogs({ page, size, type: activeType.value || undefined }),
)

function changeType(value) {
  activeType.value = value
  refresh()
}

onMounted(async () => {
  refresh()
  try {
    summary.value = await userApi.getPoints()
  } catch {
    /* 错误提示由请求拦截器统一处理 */
  }
})
</script>

<template>
  <div class="page">
    <PageHeader title="积分明细" />
    <div class="page-body">
      <div class="balance-card">
        <div class="balance-label">当前积分余额</div>
        <div class="balance-value">{{ summary?.balance ?? 0 }}</div>
        <div class="balance-row">
          <span>累计收入<b>{{ summary?.totalIncome ?? 0 }}</b></span>
          <span>累计支出<b>{{ summary?.totalExpense ?? 0 }}</b></span>
          <span>撤销扣回<b>{{ summary?.totalRevoked ?? 0 }}</b></span>
        </div>
      </div>

      <div class="chip-row" style="margin-top: 16px">
        <div
          v-for="item in typeOptions"
          :key="item.value"
          class="chip"
          :class="{ active: activeType === item.value }"
          @click="changeType(item.value)"
        >
          {{ item.label }}
        </div>
      </div>

      <el-skeleton v-if="loading && list.length === 0" :rows="4" animated />

      <EmptyState v-else-if="!loading && list.length === 0" text="暂无积分明细" />

      <template v-else>
        <p class="muted small" style="margin: 0 4px 10px">共 {{ total }} 条明细</p>
        <div v-for="item in list" :key="item.logId" class="list-card">
          <div class="list-top">
            <span class="list-title">{{ pointsTypeText[item.type] || item.type }}</span>
            <span class="points" :class="{ minus: item.change < 0 }">{{ signedPoints(item.change) }}</span>
          </div>
          <div class="list-meta">
            <span>{{ bizTypeText[item.bizType] || item.bizType }}</span>
            <span v-if="item.bizNo">单号 {{ item.bizNo }}</span>
          </div>
          <div v-if="item.remark" class="small" style="margin-top: 6px; color: #52606d">{{ item.remark }}</div>
          <div class="list-foot">
            <span>{{ formatTime(item.createdAt) }}</span>
            <span>余额 {{ item.balanceAfter }}</span>
          </div>
        </div>

        <div class="load-more">
          <el-button v-if="!finished" :loading="loading" round @click="loadMore">加载更多</el-button>
          <span v-else class="muted small">没有更多了</span>
        </div>
      </template>
    </div>
  </div>
</template>
