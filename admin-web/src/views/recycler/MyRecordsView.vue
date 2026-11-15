<template>
  <div class="page">
    <PageHeader
      title="我的流水"
      description="只显示本人经手的投递记录。5 分钟内可自助撤销，超时需管理员授权。"
    >
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Aim" @click="router.push('/workbench/identify')">识别用户</el-button>
      </template>
    </PageHeader>

    <section class="panel">
      <div class="toolbar">
        <div class="toolbar__filters">
          <el-date-picker
            v-model="filters.range"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
          <el-select v-model="filters.categoryId" placeholder="全部类别" clearable style="width: 140px">
            <el-option
              v-for="category in categories"
              :key="category.categoryId"
              :label="category.name"
              :value="category.categoryId"
            />
          </el-select>
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="正常" value="NORMAL" />
            <el-option label="已撤销" value="REVOKED" />
          </el-select>
          <el-button type="primary" plain @click="applyFilters">查询</el-button>
          <el-button text @click="resetFilters">重置</el-button>
        </div>
        <span class="muted">共 {{ total }} 条流水</span>
      </div>

      <div class="table-wrap">
        <el-table
          :data="records"
          v-loading="loading"
          :row-class-name="rowClassName"
          empty-text="还没有经手流水，识别用户后开始第一笔登记"
        >
          <el-table-column label="投递时间" width="165">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="流水号" width="160">
            <template #default="{ row }"><span class="mono">{{ row.recordNo }}</span></template>
          </el-table-column>
          <el-table-column label="用户" min-width="150">
            <template #default="{ row }">
              <div class="cell-title">{{ row.userNickname }}</div>
              <div class="cell-sub mono">{{ row.userPhoneMasked }}</div>
            </template>
          </el-table-column>
          <el-table-column label="类别" width="110">
            <template #default="{ row }">{{ row.categoryName }}</template>
          </el-table-column>
          <el-table-column label="重量" width="100" align="right">
            <template #default="{ row }"><span class="num">{{ formatWeight(row.weight) }}</span> kg</template>
          </el-table-column>
          <el-table-column label="积分" width="90" align="right">
            <template #default="{ row }"><span class="amount-pos num">+{{ row.points }}</span></template>
          </el-table-column>
          <el-table-column label="状态" min-width="150">
            <template #default="{ row }">
              <StatusTag :value="row.status" kind="delivery" />
              <div v-if="row.status === 'REVOKED'" class="cell-sub">
                {{ row.revokeReason }}<br />
                <span class="faint">{{ formatDateTime(row.revokedAt) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="right">
            <template #default="{ row }">
              <template v-if="row.status === 'NORMAL'">
                <el-tooltip
                  :disabled="row.revocable"
                  content="超过 5 分钟，需管理员授权撤销"
                  placement="top"
                >
                  <span>
                    <el-button link type="danger" :disabled="!row.revocable" @click="openRevoke(row)">
                      撤销
                    </el-button>
                  </span>
                </el-tooltip>
              </template>
              <span v-else class="faint">—</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next, total"
          background
          @current-change="load"
        />
      </div>
    </section>

    <el-dialog v-model="revokeVisible" title="撤销投递流水" width="480px" align-center>
      <div class="revoke-summary">
        <div class="revoke-row"><span>流水号</span><b class="mono">{{ target?.recordNo }}</b></div>
        <div class="revoke-row"><span>用户</span><b>{{ target?.userNickname }} · {{ target?.userPhoneMasked }}</b></div>
        <div class="revoke-row"><span>内容</span><b>{{ target?.categoryName }} {{ formatWeight(target?.weight) }} kg</b></div>
        <div class="revoke-row"><span>本笔积分</span><b class="text-danger">+{{ target?.points }}</b></div>
        <div class="revoke-row"><span>登记时间</span><b>{{ formatDateTime(target?.createdAt) }}</b></div>
      </div>

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="撤销后将从用户余额扣回本笔积分"
        description="该操作会写入积分明细并记录撤销人，用户余额不足时无法撤销，需联系管理员处理。"
        class="revoke-alert"
      />

      <el-form label-position="top">
        <el-form-item label="撤销原因（10–100 字）" required>
          <el-input
            v-model="reason"
            type="textarea"
            :rows="3"
            maxlength="100"
            show-word-limit
            placeholder="例如：重量录入有误，已重新称重"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="revokeVisible = false">取消</el-button>
        <el-button type="danger" :loading="revoking" @click="submitRevoke">确认撤销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Aim, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { listMyDeliveries, revokeMyDelivery } from '@/api/delivery'
import { listCategories } from '@/api/category'
import { formatDateTime, formatWeight } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const records = ref([])
const categories = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const filters = reactive({ range: [], categoryId: null, status: '' })

const revokeVisible = ref(false)
const revokeTarget = ref(null)
const revoking = ref(false)
const reason = ref('')

const target = ref(null)

function rowClassName({ row }) {
  return row.status === 'REVOKED' ? 'row--muted' : ''
}

function buildParams() {
  const params = { page: page.value, size }
  if (filters.range?.length === 2) {
    params.from = filters.range[0]
    params.to = filters.range[1]
  }
  if (filters.categoryId) params.categoryId = filters.categoryId
  if (filters.status) params.status = filters.status
  return params
}

async function load() {
  loading.value = true
  try {
    const data = await listMyDeliveries(buildParams())
    records.value = data.list
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
  filters.range = []
  filters.categoryId = null
  filters.status = ''
  applyFilters()
}

function openRevoke(row) {
  revokeTarget.value = row
  target.value = row
  reason.value = ''
  revokeVisible.value = true
}

async function submitRevoke() {
  const text = reason.value.trim()
  if (text.length < 10 || text.length > 100) {
    ElMessage.warning('撤销原因需为 10–100 字')
    return
  }
  revoking.value = true
  try {
    const data = await revokeMyDelivery(revokeTarget.value.recordId, { reason: text })
    ElMessage.success(`流水已撤销，已扣回 ${data.refundPoints} 积分`)
    revokeVisible.value = false
    await load()
  } finally {
    revoking.value = false
  }
}

onMounted(async () => {
  const [cats] = await Promise.all([listCategories(), load()])
  categories.value = cats
})
</script>

<style scoped>
.revoke-summary {
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: 11px;
  background: #fbfdfb;
}

.revoke-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 5px 0;
  font-size: 12.5px;
}

.revoke-row span {
  color: var(--ink-500);
  flex: none;
}

.revoke-row b {
  font-weight: 600;
  text-align: right;
}

.revoke-alert {
  margin: 16px 0 18px;
}
</style>
