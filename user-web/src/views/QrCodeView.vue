<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import QRCode from 'qrcode'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(true)
const refreshing = ref(false)
const info = ref(null)
const qrDataUrl = ref('')
const remain = ref(0)
let expiresAt = 0
let timer = null

async function load() {
  try {
    const data = await userApi.getQrCode()
    info.value = data
    qrDataUrl.value = await QRCode.toDataURL(data.qrContent, {
      width: 520,
      margin: 1,
      color: { dark: '#1f7a3d', light: '#ffffff' },
    })
    expiresAt = Date.parse(data.expiresAt) || Date.now() + data.expiresIn * 1000
    remain.value = Math.max(0, Math.round((expiresAt - Date.now()) / 1000))
  } catch {
    /* 错误提示由请求拦截器统一处理 */
  } finally {
    loading.value = false
  }
}

async function refresh() {
  if (refreshing.value) return
  refreshing.value = true
  await load()
  refreshing.value = false
}

async function copyCode() {
  try {
    await navigator.clipboard.writeText(info.value.userCode)
    ElMessage.success('用户编号已复制')
  } catch {
    ElMessage.info(`用户编号：${info.value.userCode}`)
  }
}

onMounted(async () => {
  await load()
  timer = setInterval(() => {
    remain.value = Math.max(0, Math.round((expiresAt - Date.now()) / 1000))
    if (remain.value === 0) refresh()
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="page">
    <PageHeader title="身份二维码">
      <template #right>
        <button class="icon-btn" aria-label="刷新" @click="refresh">
          <el-icon><Refresh /></el-icon>
        </button>
      </template>
    </PageHeader>

    <div class="page-body">
      <el-skeleton v-if="loading" :rows="5" animated />

      <div v-else class="card qr-card">
        <p class="qr-hint">请回收员扫描此二维码，或直接报下方用户编号</p>
        <img v-if="qrDataUrl" :src="qrDataUrl" alt="身份二维码" />
        <div class="user-code">{{ info?.userCode }}</div>
        <el-button text type="primary" @click="copyCode">复制用户编号</el-button>
        <p class="muted small">{{ remain }} 秒后自动刷新</p>
        <p class="muted small">屏幕调亮一些，扫码更容易成功</p>
      </div>

      <div class="card" style="margin-top: 14px">
        <div class="detail-row">
          <span class="label">二维码有效期</span>
          <span class="value">10 分钟</span>
        </div>
        <div class="detail-row">
          <span class="label">用户编号</span>
          <span class="value">{{ info?.userCode || '—' }}</span>
        </div>
        <div class="detail-row">
          <span class="label">使用说明</span>
          <span class="value">二维码过期后会自动刷新，请勿截图保存</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.qr-hint {
  margin: 0 0 18px;
  font-size: 14px;
  color: var(--muted);
}
</style>
