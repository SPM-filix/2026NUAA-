<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { contentApi } from '@/api'
import { usePagedList } from '@/composables/usePagedList'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { announcementTypeText, formatTime } from '@/utils/format'

const router = useRouter()
const activeType = ref('NOTICE')

const typeOptions = [
  { value: 'NOTICE', label: '系统公告' },
  { value: 'KNOWLEDGE', label: '环保科普' },
]

const { list, loading, finished, loadMore, refresh } = usePagedList(({ page, size }) =>
  contentApi.getAnnouncements({ page, size, type: activeType.value }),
)

function changeType(value) {
  activeType.value = value
  refresh()
}

onMounted(() => refresh())
</script>

<template>
  <div class="page">
    <PageHeader title="公告与科普" />
    <div class="page-body">
      <div class="chip-row">
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

      <EmptyState v-else-if="!loading && list.length === 0" text="暂无内容" />

      <template v-else>
        <div v-for="item in list" :key="item.announcementId" class="list-card" @click="router.push(`/announcements/${item.announcementId}`)">
          <div class="list-top">
            <span class="list-title">{{ item.title }}</span>
            <el-tag size="small" :type="item.type === 'NOTICE' ? 'primary' : 'success'" effect="light" round>
              {{ announcementTypeText[item.type] }}
            </el-tag>
          </div>
          <p v-if="item.summary" class="summary">{{ item.summary }}</p>
          <div class="list-foot">
            <span>{{ formatTime(item.publishedAt) }}</span>
            <el-icon><ArrowRight /></el-icon>
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
.summary {
  display: -webkit-box;
  margin: 8px 0 0;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.6;
  color: var(--muted);
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>
