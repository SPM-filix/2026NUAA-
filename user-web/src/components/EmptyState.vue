<script setup>
defineProps({
  text: { type: String, default: '暂无数据' },
})
</script>

<template>
  <div class="empty-state">
    <div class="empty-icon">
      <el-icon><Box /></el-icon>
    </div>
    <p>{{ text }}</p>
    <slot />
  </div>
</template>
