<script setup>
import { onMounted, ref } from 'vue'
import { userApi } from '@/api'
import { usePagedList } from '@/composables/usePagedList'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { formatTime, targetTypeText } from '@/utils/format'

const expandedId = ref(null)
const { list, loading, finished, loadMore, refresh } = usePagedList(({ page, size }) =>
  userApi.getAppeals({ page, size }),
)

function toggle(item) {
  expandedId.value = expandedId.value === item.appealId ? null : item.appealId
}

onMounted(() => refresh())
</script>

<template>
  <div class="page">
    <PageHeader title="我的申诉" />
    <div class="page-body">
      <el-skeleton v-if="loading && list.length === 0" :rows="4" animated />

      <EmptyState v-else-if="!loading && list.length === 0" text="还没有提交过申诉" />

      <template v-else>
        <div v-for="item in list" :key="item.appealId" class="list-card" @click="toggle(item)">
          <div class="list-top">
            <span class="list-title">
              {{ targetTypeText[item.targetType] || item.targetType }} · {{ item.targetNo || item.targetId }}
            </span>
            <StatusTag :status="item.status" kind="appeal" />
          </div>
          <p class="description">{{ item.description }}</p>
          <div class="list-foot">
            <span>{{ formatTime(item.createdAt) }}</span>
            <span class="muted small">{{ expandedId === item.appealId ? '收起' : '查看结论' }}</span>
          </div>

          <div v-if="expandedId === item.appealId" class="conclusion">
            <template v-if="item.conclusion">
              <div class="conclusion-title">处理结论</div>
              <p>{{ item.conclusion }}</p>
              <div class="muted small">处理时间：{{ formatTime(item.handledAt) }}</div>
            </template>
            <span v-else class="muted small">管理员尚未处理，请耐心等待。</span>
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
.description {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #52606d;
}

.conclusion {
  padding: 12px;
  margin-top: 12px;
  font-size: 13px;
  background: #f7faf8;
  border-radius: 10px;
}

.conclusion-title {
  margin-bottom: 6px;
  font-weight: 600;
}

.conclusion p {
  margin: 0 0 6px;
  line-height: 1.6;
}
</style>
