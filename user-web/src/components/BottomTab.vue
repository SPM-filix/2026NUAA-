<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const items = [
  { path: '/', label: '首页', icon: 'HomeFilled', match: (path) => path === '/' },
  { path: '/mall', label: '商城', icon: 'ShoppingBag', match: (path) => path.startsWith('/mall') },
  { path: '/me', label: '我的', icon: 'User', match: (path) => path === '/me' || path.startsWith('/me/') },
]
const activePath = computed(() => items.find((item) => item.match(route.path))?.path)
</script>

<template>
  <nav class="bottom-tab">
    <router-link
      v-for="item in items"
      :key="item.path"
      :to="item.path"
      :class="{ active: activePath === item.path }"
    >
      <el-icon><component :is="item.icon" /></el-icon>
      <span>{{ item.label }}</span>
    </router-link>
  </nav>
</template>
