<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { catalogApi, userApi } from '@/api'
import { usePagedList } from '@/composables/usePagedList'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { formatTime } from '@/utils/format'

const router = useRouter()
const categories = ref([])
const filters = reactive({ categoryId: '', range: [] })

const { list, total, loading, finished, load, loadMore, refresh } = usePagedList(({ page, size }) =>
  userApi.getDeliveries({
    page,
    size,
    categoryId: filters.categoryId || undefined,
    from: filters.range?.[0] ? `${filters.range[0]}T00:00:00+08:00` : undefined,
    to: filters.range?.[1] ? `${filters.range[1]}T23:59:59+08:00` : undefined,
  }),
)

onMounted(async () => {
  refresh()
  try {
    categories.value = await catalogApi.getCategories({ enabledOnly: true })
  } catch {
    /* 错误提示由请求拦截器统一处理 */
  }
})
</script>

<template>
  <div class="page">
    <PageHeader title="投递记录" />
    <div class="page-body">
      <div class="filters">
        <el-select v-model="filters.categoryId" placeholder="全部类别" clearable size="default" style="width: 120px" @change="refresh">
          <el-option v-for="item in categories" :key="item.categoryId" :label="item.name" :value="item.categoryId" />
        </el-select>
        <el-date-picker
          v-model="filters.range"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="default"
          style="flex: 1"
          @change="refresh"
        />
      </div>

      <el-skeleton v-if="loading && list.length === 0" :rows="4" animated />

      <EmptyState v-else-if="!loading && list.length === 0" text="该条件下还没有投递记录">
        <el-button type="primary" round size="small" @click="router.push('/me/qrcode')">去投放点出示身份码</el-button>
      </EmptyState>

      <template v-else>
        <p class="muted small" style="margin: 0 4px 10px">共 {{ total }} 条记录</p>
        <div
          v-for="item in list"
          :key="item.recordId"
          class="list-card"
          :class="{ revoked: item.status === 'REVOKED' }"
          @click="router.push(`/me/deliveries/${item.recordId}`)"
        >
          <div class="list-top">
            <span class="list-title">{{ item.categoryName }} · {{ item.weight }} kg</span>
            <span class="points">{{ item.points }} 积分</span>
          </div>
          <div class="list-meta">
            <span>{{ item.recyclerName }}</span>
            <span v-if="item.siteName">{{ item.siteName }}</span>
            <span>{{ item.recordNo }}</span>
          </div>
          <div class="list-foot">
            <span>{{ formatTime(item.createdAt) }}</span>
            <StatusTag :status="item.status" kind="delivery" />
          </div>
          <div v-if="item.status === 'REVOKED' && item.revokeReason" class="revoke-box">
            撤销原因：{{ item.revokeReason }}
            <div class="small" style="margin-top: 4px">撤销时间：{{ formatTime(item.revokedAt) }}</div>
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

<style scoped>
.filters {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 14px;
}
</style>
