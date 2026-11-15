<template>
  <div class="page">
    <PageHeader
      title="兑换核销"
      description="输入或扫描用户出示的 8 位核销码，核对商品与用户后确认核销。"
    >
      <template #actions>
        <el-button :icon="Refresh" @click="loadRecent">刷新核销记录</el-button>
      </template>
    </PageHeader>

    <section class="panel redeem-panel">
      <div class="redeem-head">
        <div class="code-field">
          <label>核销码</label>
          <el-input
            v-model="code"
            size="large"
            maxlength="8"
            placeholder="输入 8 位核销码，如 A7K2M9QX"
            class="code-input"
            :prefix-icon="Ticket"
            clearable
            @input="normalizeCode"
            @keyup.enter="search"
          />
        </div>
        <el-button type="primary" size="large" :loading="loadingPreview" :icon="Search" @click="search">
          查询订单
        </el-button>
      </div>

      <p class="code-hint">核销码不区分大小写，扫描枪输入后会自动去除首尾空格。</p>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        :closable="false"
        class="redeem-error"
      />

      <transition name="fade-slide" mode="out-in">
        <div v-if="success" key="success" class="success-box">
          <span class="success-box__check"><el-icon><Check /></el-icon></span>
          <h2>核销成功</h2>
          <p class="success-box__product">{{ success.productName }} × {{ success.quantity }}</p>
          <p class="success-box__meta">
            订单 <span class="mono">{{ success.orderNo }}</span> · 用户 {{ success.userNickname }}
          </p>
          <p class="success-box__time">核销时间 {{ formatDateTime(success.verifiedAt) }}</p>
          <div class="success-box__actions">
            <el-button type="primary" size="large" @click="startNext">继续核销下一单</el-button>
          </div>
        </div>

        <div v-else-if="preview" key="preview" class="preview-box">
          <div class="preview-main">
            <img class="preview-cover" :src="preview.productCoverUrl" :alt="preview.productName" />
            <div class="preview-info">
              <span class="preview-badge">待核销</span>
              <h2>{{ preview.productName }}</h2>
              <p class="preview-sub">数量 × {{ preview.quantity }} · 消耗 {{ preview.pointsCost }} 积分</p>
              <div class="preview-meta">
                <span>订单号 <b class="mono">{{ preview.orderNo }}</b></span>
                <span>用户 <b>{{ preview.userNickname }}</b> · {{ preview.userPhoneMasked }}</span>
                <span>下单时间 {{ formatDateTime(preview.createdAt) }}</span>
                <span>核销码有效期至 {{ formatDateTime(preview.redeemCodeExpireAt) }}</span>
              </div>
            </div>
            <div class="preview-side">
              <el-button type="primary" size="large" :loading="redeeming" @click="confirmRedeem">
                确认核销
              </el-button>
              <el-button size="large" @click="reset">重新输入</el-button>
            </div>
          </div>
          <el-alert
            type="warning"
            :closable="false"
            show-icon
            title="请先核对实物与用户身份"
            description="核销后订单状态变为「已完成」，核销码立即失效，不可撤销。"
          />
        </div>

        <div v-else key="guide" class="guide-box">
          <div class="guide-steps">
            <div class="guide-step">
              <span class="guide-step__no">01</span>
              <h4>输入核销码</h4>
              <p>使用扫描枪扫描用户核销码，或手动输入 8 位字符。</p>
            </div>
            <div class="guide-step">
              <span class="guide-step__no">02</span>
              <h4>核对订单</h4>
              <p>确认商品、数量、用户与有效期，避免核销错误订单。</p>
            </div>
            <div class="guide-step">
              <span class="guide-step__no">03</span>
              <h4>确认核销</h4>
              <p>核销成功后把实物交给用户，再将订单标记为已完成。</p>
            </div>
          </div>
        </div>
      </transition>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div>
          <h3 class="panel-title">我的核销记录</h3>
          <p class="panel-sub">仅显示本人核销的订单，最近 5 条</p>
        </div>
        <el-button text type="primary" @click="router.push('/workbench/records')">查看投递流水</el-button>
      </div>

      <div class="table-wrap">
        <el-table :data="recent" v-loading="recentLoading" empty-text="还没有核销记录">
          <el-table-column label="核销时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.verifiedAt) }}</template>
          </el-table-column>
          <el-table-column label="订单号" width="180">
            <template #default="{ row }"><span class="mono">{{ row.orderNo }}</span></template>
          </el-table-column>
          <el-table-column prop="userNickname" label="用户" min-width="110" />
          <el-table-column prop="productName" label="商品" min-width="160" />
          <el-table-column label="数量" width="80" align="center">
            <template #default="{ row }">× {{ row.quantity }}</template>
          </el-table-column>
          <el-table-column label="消耗积分" width="110" align="right">
            <template #default="{ row }"><span class="amount-neg num">-{{ row.pointsCost }}</span></template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Refresh, Search, Ticket } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { listMyRedemptions, previewRedeem, redeem } from '@/api/exchange'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

const code = ref('')
const preview = ref(null)
const success = ref(null)
const errorMessage = ref('')
const loadingPreview = ref(false)
const redeeming = ref(false)
const recent = ref([])
const recentLoading = ref(false)

function normalizeCode() {
  code.value = code.value.toUpperCase().replace(/\s+/g, '').slice(0, 8)
}

function reset() {
  preview.value = null
  success.value = null
  errorMessage.value = ''
  code.value = ''
}

async function search() {
  normalizeCode()
  errorMessage.value = ''
  success.value = null
  preview.value = null
  if (!code.value) {
    errorMessage.value = '请输入核销码'
    return
  }
  if (code.value.length !== 8) {
    errorMessage.value = '核销码为 8 位大写字母或数字'
    return
  }

  loadingPreview.value = true
  try {
    preview.value = await previewRedeem(code.value)
  } catch (error) {
    errorMessage.value = error?.message || '核销码无效或不可核销'
  } finally {
    loadingPreview.value = false
  }
}

async function confirmRedeem() {
  if (!preview.value || redeeming.value) return
  try {
    await ElMessageBox.confirm(
      `确认核销「${preview.value.productName} × ${preview.value.quantity}」？核销后订单立即完成，操作不可撤销。`,
      '确认核销',
      {
        confirmButtonText: '确认核销',
        cancelButtonText: '再核对一下',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  redeeming.value = true
  try {
    success.value = await redeem(code.value)
    preview.value = null
    code.value = ''
    errorMessage.value = ''
    await loadRecent()
  } catch (error) {
    errorMessage.value = error?.message || '核销失败，请重试'
  } finally {
    redeeming.value = false
  }
}

function startNext() {
  reset()
}

async function loadRecent() {
  recentLoading.value = true
  try {
    const data = await listMyRedemptions({ page: 1, size: 5 })
    recent.value = data.list
  } finally {
    recentLoading.value = false
  }
}

onMounted(loadRecent)
</script>

<style scoped>
.redeem-panel {
  padding: 24px 26px 26px;
}

.redeem-head {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.code-field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  flex: 1;
  min-width: 280px;
  max-width: 560px;
}

.code-field label {
  color: var(--ink-700);
  font-size: 12.5px;
  font-weight: 600;
}

.code-input :deep(.el-input__inner) {
  font-family: var(--font-mono);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.redeem-head .el-button {
  height: 44px;
  min-width: 128px;
}

.code-hint {
  margin-top: 8px;
  color: var(--ink-400);
  font-size: 11.5px;
}

.redeem-error {
  margin-top: 18px;
}

.guide-box {
  margin-top: 26px;
  padding-top: 24px;
  border-top: 1px solid var(--line);
}

.guide-steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.guide-step {
  padding: 18px 20px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fbfdfb;
}

.guide-step__no {
  display: inline-block;
  color: var(--brand-500);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
}

.guide-step h4 {
  margin-top: 6px;
  font-size: 13.5px;
  font-weight: 600;
}

.guide-step p {
  margin-top: 4px;
  color: var(--ink-500);
  font-size: 11.5px;
  line-height: 1.7;
}

.preview-box {
  margin-top: 24px;
  padding-top: 22px;
  border-top: 1px solid var(--line);
}

.preview-main {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.preview-cover {
  width: 88px;
  height: 88px;
  border-radius: 14px;
  object-fit: cover;
  border: 1px solid var(--line);
  background: var(--brand-50);
  flex: none;
}

.preview-info {
  flex: 1;
  min-width: 240px;
}

.preview-badge {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 999px;
  background: var(--amber-50);
  color: var(--amber-700);
  font-size: 10.5px;
  font-weight: 600;
}

.preview-info h2 {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 650;
}

.preview-sub {
  margin-top: 2px;
  color: var(--ink-500);
  font-size: 12.5px;
}

.preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 20px;
  margin-top: 10px;
  color: var(--ink-500);
  font-size: 11.5px;
}

.preview-meta b {
  color: var(--ink-700);
  font-weight: 600;
}

.preview-side {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-left: auto;
}

.preview-side .el-button {
  width: 132px;
  height: 42px;
}

.success-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 34px 20px 26px;
  margin-top: 20px;
  border-top: 1px solid var(--line);
  text-align: center;
}

.success-box__check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--brand-100);
  color: var(--brand-600);
  font-size: 30px;
  margin-bottom: 16px;
}

.success-box h2 {
  font-size: 21px;
  font-weight: 650;
}

.success-box__product {
  margin-top: 8px;
  color: var(--ink-700);
  font-size: 14px;
  font-weight: 550;
}

.success-box__meta {
  margin-top: 4px;
  color: var(--ink-500);
  font-size: 12px;
}

.success-box__time {
  margin-top: 2px;
  color: var(--ink-400);
  font-size: 11.5px;
}

.success-box__actions {
  margin-top: 22px;
}

.success-box__actions .el-button {
  min-width: 190px;
  height: 44px;
}

@media (max-width: 900px) {
  .guide-steps {
    grid-template-columns: 1fr;
  }

  .preview-side {
    flex-direction: row;
    margin-left: 0;
  }
}
</style>
