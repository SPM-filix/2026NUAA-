<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { contentApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { announcementTypeText, formatTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const item = ref(null)
const error = ref('')

onMounted(async () => {
  try {
    item.value = await contentApi.getAnnouncement(route.params.id)
  } catch (err) {
    error.value = err?.message || '公告不存在'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <PageHeader title="详情" />
    <div class="page-body">
      <el-skeleton v-if="loading" :rows="6" animated />

      <EmptyState v-else-if="error" :text="error">
        <el-button round size="small" @click="router.replace('/announcements')">返回列表</el-button>
      </EmptyState>

      <article v-else-if="item" class="card">
        <el-tag size="small" :type="item.type === 'NOTICE' ? 'primary' : 'success'" effect="light" round>
          {{ announcementTypeText[item.type] }}
        </el-tag>
        <h1 class="detail-title">{{ item.title }}</h1>
        <p class="muted small">{{ formatTime(item.publishedAt) }}</p>
        <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" class="cover" />
        <div class="content">{{ item.content }}</div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.detail-title {
  margin: 10px 0 6px;
  font-size: 21px;
  line-height: 1.4;
}

.cover {
  display: block;
  width: 100%;
  margin: 14px 0;
  border-radius: 12px;
}

.content {
  margin-top: 14px;
  font-size: 15px;
  line-height: 1.85;
  color: #3e4c59;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
