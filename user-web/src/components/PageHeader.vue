<script setup>
import { useRouter } from 'vue-router'

defineProps({
  title: { type: String, default: '' },
  back: { type: Boolean, default: true },
})

const router = useRouter()

function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace('/')
}
</script>

<template>
  <header class="page-header">
    <button v-if="back" class="icon-btn" aria-label="返回" @click="goBack">
      <el-icon><ArrowLeft /></el-icon>
    </button>
    <span v-else class="icon-btn" />
    <span class="page-title">{{ title }}</span>
    <div class="header-right">
      <slot name="right" />
    </div>
  </header>
</template>
