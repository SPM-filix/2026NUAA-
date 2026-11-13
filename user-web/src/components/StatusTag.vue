<script setup>
import { computed } from 'vue'
import { appealStatusText, deliveryStatusText, orderStatusText } from '@/utils/format'

const props = defineProps({
  status: { type: String, default: '' },
  kind: { type: String, default: 'delivery' },
})

const maps = {
  delivery: {
    NORMAL: { text: deliveryStatusText.NORMAL, type: 'success' },
    REVOKED: { text: deliveryStatusText.REVOKED, type: 'info' },
  },
  order: {
    PENDING: { text: orderStatusText.PENDING, type: 'warning' },
    COMPLETED: { text: orderStatusText.COMPLETED, type: 'success' },
    CANCELLED: { text: orderStatusText.CANCELLED, type: 'info' },
  },
  appeal: {
    PENDING: { text: appealStatusText.PENDING, type: 'warning' },
    ACCEPTED: { text: appealStatusText.ACCEPTED, type: 'success' },
    REJECTED: { text: appealStatusText.REJECTED, type: 'danger' },
  },
}

const meta = computed(() => maps[props.kind]?.[props.status] || { text: props.status, type: 'info' })
</script>

<template>
  <el-tag :type="meta.type" size="small" effect="light" round>{{ meta.text }}</el-tag>
</template>
