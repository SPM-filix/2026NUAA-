<template>
  <el-tag :type="tagType" size="small" effect="light" round>{{ label }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'
import {
  ANNOUNCEMENT_STATUS,
  ANNOUNCEMENT_TYPE,
  APPEAL_STATUS,
  DELIVERY_STATUS,
  ENABLE_STATUS,
  ORDER_STATUS,
  PRODUCT_STATUS,
  RECYCLER_STATUS,
  STATUS_TAG_TYPE,
  USER_STATUS
} from '@/utils/dict'

const props = defineProps({
  value: { type: String, default: '' },
  kind: { type: String, default: 'user' }
})

const MAPS = {
  user: USER_STATUS,
  recycler: RECYCLER_STATUS,
  delivery: DELIVERY_STATUS,
  order: ORDER_STATUS,
  product: PRODUCT_STATUS,
  enable: ENABLE_STATUS,
  announcement: ANNOUNCEMENT_STATUS,
  announcementType: ANNOUNCEMENT_TYPE,
  appeal: APPEAL_STATUS
}

const EXTRA_TAG_TYPE = {
  NOTICE: 'primary',
  KNOWLEDGE: 'success',
  PENDING: 'warning',
  ACCEPTED: 'success',
  REJECTED: 'danger',
  PUBLISHED: 'success',
  OFFLINE: 'info'
}

const label = computed(() => MAPS[props.kind]?.[props.value] || props.value || '—')
const tagType = computed(() => EXTRA_TAG_TYPE[props.value] || STATUS_TAG_TYPE[props.value] || 'info')
</script>
