<template>
  <div class="page">
    <PageHeader
      title="兑换订单"
      description="查看全部兑换订单。待核销订单可取消，取消时退回积分并恢复库存；已核销订单只能查看。"
    >
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <section class="panel">
      <div class="toolbar">
        <div class="toolbar__filters">
          <el-input
            v-model="filters.orderNo"
            placeholder="搜索订单号"
            clearable
            :prefix-icon="Search"
            style="width: 220px"
            @keyup.enter="applyFilters"
          />
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px" @change="applyFilters">
            <el-option label="待核销" value="PENDING" />
            <el-option label="已核销" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
          <el-button type="primary" plain @click="applyFilters">查询</el-button>
          <el-button text @click="resetFilters">重置</el-button>
        </div>
        <span class="muted">共 {{ total }} 笔订单</span>
      </div>

      <div class="table-wrap">
        <el-table :data="orders" v-loading="loading" empty-text="没有符合条件的订单">
          <el-table-column label="订单号" width="165">
            <template #default="{ row }"><span class="mono">{{ row.orderNo }}</span></template>
          </el-table-column>
          <el-table-column label="用户" width="110">
            <template #default="{ row }">{{ row.userNickname }}</template>
          </el-table-column>
          <el-table-column label="商品" min-width="170">
            <template #default="{ row }">
              <div class="cell-title">{{ row.productName }}</div>
              <div class="cell-sub">数量 × {{ row.quantity }}</div>
            </template>
          </el-table-column>
          <el-table-column label="消耗积分" width="100" align="right">
            <template #default="{ row }">
              <span class="amount-neg num">-{{ formatNumber(row.pointsCost) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }"><StatusTag :value="row.status" kind="order" /></template>
          </el-table-column>
          <el-table-column label="核销 / 取消信息" min-width="170">
            <template #default="{ row }">
              <template v-if="row.status === 'COMPLETED'">
                <div>{{ row.verifierName || '—' }}</div>
                <div class="cell-sub">{{ formatDateTime(row.verifiedAt) }}</div>
              </template>
              <template v-else-if="row.status === 'CANCELLED'">
                <div class="text-danger">{{ row.cancelReason || '已取消' }}</div>
              </template>
              <template v-else>
                <div class="mono">{{ row.redeemCode }}</div>
                <div class="cell-sub">待核销</div>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" width="150">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="125" align="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button v-if="row.status === 'PENDING'" link type="danger" @click="openCancel(row)">取消</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="20"
          :total="total"
          layout="prev, pager, next, total"
          background
          @current-change="load"
        />
      </div>

      <div class="panel-note">
        <el-icon><InfoFilled /></el-icon>
        核销码有效期 30 天；超期未核销的待核销订单会出现在看板待办中，建议取消并退回积分。
      </div>
    </section>

    <el-drawer v-model="detailVisible" title="订单详情" size="470px">
      <template v-if="detail">
        <div class="detail-head">
          <img v-if="detail.productCoverUrl" class="thumb thumb--lg" :src="detail.productCoverUrl" alt="" />
          <div>
            <h3>{{ detail.productName }}</h3>
            <p class="muted">数量 × {{ detail.quantity }} · {{ formatNumber(detail.pointsCost) }} 积分</p>
            <StatusTag :value="detail.status" kind="order" />
          </div>
        </div>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单号"><span class="mono">{{ detail.orderNo }}</span></el-descriptions-item>
          <el-descriptions-item label="用户">{{ detail.userNickname }}（ID {{ detail.userId }}）</el-descriptions-item>
          <el-descriptions-item label="核销码"><span class="mono">{{ detail.redeemCode }}</span></el-descriptions-item>
          <el-descriptions-item label="核销码有效期">{{ formatDateTime(detail.redeemCodeExpireAt) }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="核销站点">{{ detail.siteName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="核销人">{{ detail.verifierName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="核销时间">{{ formatDateTime(detail.verifiedAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.cancelReason" label="取消原因">
            <span class="text-danger">{{ detail.cancelReason }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>

    <el-dialog v-model="cancelVisible" title="取消兑换订单" width="500px" align-center>
      <div class="cancel-summary">
        <div class="cancel-row"><span>订单号</span><b class="mono">{{ cancelTarget?.orderNo }}</b></div>
        <div class="cancel-row"><span>用户</span><b>{{ cancelTarget?.userNickname }}</b></div>
        <div class="cancel-row"><span>商品</span><b>{{ cancelTarget?.productName }} × {{ cancelTarget?.quantity }}</b></div>
        <div class="cancel-row"><span>退回积分</span><b class="amount-pos">+{{ cancelTarget?.pointsCost }} 分</b></div>
      </div>

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="取消将退回积分并恢复库存"
        description="仅待核销订单可以取消，操作会写入取消原因与操作人。"
        class="cancel-alert"
      />

      <el-form label-position="top">
        <el-form-item label="取消原因（10–200 字）" required>
          <el-input
            v-model="cancelReason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="例如：商品缺货，站点无法履约，退回积分"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="cancelVisible = false">取消</el-button>
        <el-button type="danger" :loading="cancelling" @click="submitCancel">确认取消订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { cancelOrder, getOrder, listOrders } from '@/api/exchange'
import { formatDateTime, formatNumber } from '@/utils/format'

const orders = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const filters = reactive({ status: '', orderNo: '' })

const detailVisible = ref(false)
const detail = ref(null)

const cancelVisible = ref(false)
const cancelTarget = ref(null)
const cancelReason = ref('')
const cancelling = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await listOrders({
      page: page.value,
      size: 20,
      status: filters.status || undefined,
      orderNo: filters.orderNo.trim() || undefined
    })
    orders.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 1
  load()
}

function resetFilters() {
  filters.status = ''
  filters.orderNo = ''
  applyFilters()
}

async function openDetail(row) {
  detail.value = await getOrder(row.orderId)
  detailVisible.value = true
}

function openCancel(row) {
  cancelTarget.value = row
  cancelReason.value = ''
  cancelVisible.value = true
}

async function submitCancel() {
  const reason = cancelReason.value.trim()
  if (reason.length < 10 || reason.length > 200) {
    ElMessage.warning('取消原因需为 10–200 字')
    return
  }
  cancelling.value = true
  try {
    const data = await cancelOrder(cancelTarget.value.orderId, { reason })
    ElMessage.success(`订单已取消，退回 ${data.refundPoints} 积分并恢复库存 ${data.restoredStock} 件`)
    cancelVisible.value = false
    await load()
  } finally {
    cancelling.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.panel-note {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 13px 22px;
  border-top: 1px solid var(--line);
  background: #fbfdfb;
  color: var(--ink-500);
  font-size: 11.5px;
}

.detail-head {
  display: flex;
  gap: 16px;
  padding-bottom: 20px;
  margin-bottom: 18px;
  border-bottom: 1px solid var(--line);
}

.detail-head h3 {
  font-size: 16px;
  font-weight: 650;
}

.detail-head p {
  margin: 3px 0 8px;
  font-size: 12px;
}

.cancel-summary {
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: 11px;
  background: #fbfdfb;
}

.cancel-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 5px 0;
  font-size: 12.5px;
}

.cancel-row span {
  color: var(--ink-500);
  flex: none;
}

.cancel-row b {
  font-weight: 600;
  text-align: right;
}

.cancel-alert {
  margin: 16px 0 18px;
}
</style>
