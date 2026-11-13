<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import BottomTab from '@/components/BottomTab.vue'

const route = useRoute()
const showTab = computed(() => Boolean(route.meta.tab))
</script>

<template>
  <div class="app-shell" :class="{ 'with-tab': showTab }">
    <router-view />
  </div>
  <BottomTab v-if="showTab" />
</template>
