<template>
  <div class="page">
    <PageHeader
      title="流水审计"
      description="查看全量投递流水，按回收员、类别与时间筛选。异常流水会标红，管理员可授权撤销。"
    >
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <section class="panel">
      <div class="toolbar">
        <div class="toolbar__filters">
          <el-select v-model="filters.recyclerId" placeholder="全部回收员" clearable filterable style="width: 160px">
            <el-option
              v-for="recycler in recyclers"
              :key="recycler.recyclerId"
              :label="recycler.name"
              :value="recycler.recyclerId"
            />
          </el-select>
          <el-select v-model="filters.categoryId" placeholder="全部类别" clearable style="width: 130px">
            <el-option
              v-for="category in categories"
              :key="category.categoryId"
              :label="category.name"
              :value="category.categoryId"
            />
          </el-select>
          <el-date-picker
            v-model="filters.range"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 250px"
          />
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="正常" value="NORMAL" />
            <el-option label="已撤销" value="REVOKED" />
          </el-select>
          <el-checkbox v-model="filters.abnormalOnly">只看异常</el-checkbox>
          <el-button type="primary" plain @click="applyFilters">查询</el-button>
          <el-button text @click="resetFilters">重置</el-button>
        </div>
      </div>

      <div class="summary-strip">
        <div class="summary-item">
          <span>筛选结果</span>
          <strong class="num">{{ summary.totalCount }}</strong>
          <small>笔流水</small>
        </div>
        <div class="summary-item">
          <span>正常流水重量</span>
          <strong class="num">{{ formatWeight(summary.totalWeight) }}</strong>
          <small>kg</small>
        </div>
        <div class="summary-item">
          <span>正常流水发放积分</span>
          <strong class="num">{{ formatNumber(summary.totalPoints) }}</strong>
          <small>分</small>
        </div>
        <div class="summary-item">
          <span>已撤销</span>
          <strong class="num text-danger">{{ summary.revokedCount }}</strong>
          <small>笔</small>
        </div>
      </div>

      <div class="table-wrap">
        <el-table
          :data="records"
          v-loading="loading"
          :row-class-name="rowClassName"
          empty-text="没有符合条件的流水"
        >
          <el-table-column label="投递时间" width="145">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="流水号" width="145">
            <template #default="{ row }"><span class="mono">{{ row.recordNo }}</span></template>
          </el-table-column>
          <el-table-column label="用户" width="115">
            <template #default="{ row }">
              <div class="cell-title">{{ row.userNickname }}</div>
              <div class="cell-sub mono">{{ row.userPhoneMasked }}</div>
            </template>
          </el-table-column>
          <el-table-column label="回收员 / 站点" width="130">
            <template #default="{ row }">
              <div>{{ row.recyclerName }}</div>
              <div class="cell-sub">{{ row.siteName }}</div>
            </template>
          </el-table-column>
          <el-table-column label="类别" width="80">
            <template #default="{ row }">{{ row.categoryName }}</template>
          </el-table-column>
          <el-table-column label="重量" width="80" align="right">
            <template #default="{ row }">
              <span class="num">{{ formatWeight(row.weight) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="积分" width="70" align="right">
            <template #default="{ row }">
              <span class="amount-pos num">+{{ row.points }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }"><StatusTag :value="row.status" kind="delivery" /></template>
          </el-table-column>
          <el-table-column label="异常标记" width="100">
            <template #default="{ row }">
              <div v-if="row.abnormalFlags?.length" class="flag-list">
                <el-tooltip
                  v-for="flag in row.abnormalFlags"
                  :key="flag"
                  :content="flagHint(flag)"
                  placement="top"
                >
                  <el-tag type="danger" size="small" effect="light">{{ ABNORMAL_FLAG[flag] || flag }}</el-tag>
                </el-tooltip>
              </div>
              <span v-else class="faint">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button
                v-if="row.status === 'NORMAL'"
                link
                type="danger"
                @click="openRevoke(row)"
              >
                授权撤销
              </el-button>
              <span v-else class="faint">已撤销</span>
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
    </section>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="流水详情" size="460px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="流水号"><span class="mono">{{ detail.recordNo }}</span></el-descriptions-item>
          <el-descriptions-item label="状态"><StatusTag :value="detail.status" kind="delivery" /></el-descriptions-item>
          <el-descriptions-item label="用户">
            {{ detail.userNickname }} · {{ detail.userPhoneMasked }}
          </el-descriptions-item>
          <el-descriptions-item label="回收员">{{ detail.recyclerName }}</el-descriptions-item>
          <el-descriptions-item label="站点">{{ detail.siteName }}</el-descriptions-item>
          <el-descriptions-item label="类别">{{ detail.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="重量">{{ formatWeight(detail.weight) }} kg</el-descriptions-item>
          <el-descriptions-item label="发放积分">
            <span class="amount-pos">+{{ detail.points }} 分</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item>
          <el-descriptions-item label="投递时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.status === 'REVOKED'" label="撤销信息">
            {{ detail.revokeReason }}<br />
            <span class="muted">{{ detail.revokedByType === 'ADMIN' ? '管理员' : '回收员' }} {{ detail.revokedBy }} · {{ formatDateTime(detail.revokedAt) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="异常标记">
            <template v-if="detail.abnormalFlags?.length">
              <el-tag v-for="flag in detail.abnormalFlags" :key="flag" type="danger" size="small" class="flag-tag">
                {{ ABNORMAL_FLAG[flag] || flag }}
              </el-tag>
            </template>
            <span v-else>未命中异常规则</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="异常规则（v0.1 写死）"
          description="单笔重量 > 50 kg；同一用户 1 小时内 ≥ 5 笔；同一回收员 10 分钟内 ≥ 10 笔。"
          class="detail-note"
        />
      </template>
    </el-drawer>

    <!-- 授权撤销 -->
    <el-dialog v-model="revokeVisible" title="授权撤销流水" width="500px" align-center>
      <div class="revoke-summary">
        <div class="revoke-row"><span>流水号</span><b class="mono">{{ revokeTarget?.recordNo }}</b></div>
        <div class="revoke-row"><span>用户</span><b>{{ revokeTarget?.userNickname }} · {{ revokeTarget?.userPhoneMasked }}</b></div>
        <div class="revoke-row"><span>回收员</span><b>{{ revokeTarget?.recyclerName }} · {{ revokeTarget?.siteName }}</b></div>
        <div class="revoke-row"><span>内容</span><b>{{ revokeTarget?.categoryName }} {{ formatWeight(revokeTarget?.weight) }} kg · +{{ revokeTarget?.points }} 分</b></div>
        <div class="revoke-row"><span>投递时间</span><b>{{ formatDateTime(revokeTarget?.createdAt) }}</b></div>
      </div>

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="撤销后将扣回用户积分并记录操作人"
        description="若用户余额不足以扣回，会扣至 0 并返回差额；该操作不影响其他流水。"
        class="revoke-alert"
      />

      <el-form label-position="top">
        <el-form-item label="授权撤销原因（10–200 字）" required>
          <el-input
            v-model="revokeReason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="说明违规或错误原因，例如：重量明显异常，经核实为重复录入"
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
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { listAdminDeliveries, revokeDeliveryAsAdmin } from '@/api/delivery'
import { listRecyclers } from '@/api/account'
import { listCategories } from '@/api/category'
import { ABNORMAL_FLAG } from '@/utils/dict'
import { formatDateTime, formatNumber, formatWeight } from '@/utils/format'

const records = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const recyclers = ref([])
const categories = ref([])
const summary = reactive({ totalCount: 0, totalWeight: 0, totalPoints: 0, revokedCount: 0 })
const filters = reactive({ recyclerId: null, categoryId: null, range: [], status: '', abnormalOnly: false })

const detailVisible = ref(false)
const detail = ref(null)

const revokeVisible = ref(false)
const revokeTarget = ref(null)
const revokeReason = ref('')
const revoking = ref(false)

function flagHint(flag) {
  return {
    OVER_WEIGHT: '单笔重量超过 50 kg',
    HIGH_FREQUENCY: '同一用户 1 小时内投递 ≥ 5 笔'
  }[flag] || flag
}

function rowClassName({ row }) {
  if (row.abnormalFlags?.length) return 'row--warn'
  if (row.status === 'REVOKED') return 'row--muted'
  return ''
}

function buildParams() {
  const params = { page: page.value, size: 20 }
  if (filters.recyclerId) params.recyclerId = filters.recyclerId
  if (filters.categoryId) params.categoryId = filters.categoryId
  if (filters.status) params.status = filters.status
  if (filters.abnormalOnly) params.abnormalOnly = true
  if (filters.range?.length === 2) {
    params.from = filters.range[0]
    params.to = filters.range[1]
  }
  return params
}

async function load() {
  loading.value = true
  try {
    const data = await listAdminDeliveries(buildParams())
    records.value = data.list
    total.value = data.total
    Object.assign(summary, data.summary)
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 1
  load()
}

function resetFilters() {
  Object.assign(filters, { recyclerId: null, categoryId: null, range: [], status: '', abnormalOnly: false })
  applyFilters()
}

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

function openRevoke(row) {
  revokeTarget.value = row
  revokeReason.value = ''
  revokeVisible.value = true
}

async function submitRevoke() {
  const reason = revokeReason.value.trim()
  if (reason.length < 10 || reason.length > 200) {
    ElMessage.warning('授权撤销原因需为 10–200 字')
    return
  }
  revoking.value = true
  try {
    const data = await revokeDeliveryAsAdmin(revokeTarget.value.recordId, { reason })
    ElMessage.success(
      data.deficit > 0
        ? `流水已撤销，用户余额不足，扣至 0，差额 ${data.deficit} 分`
        : `流水已撤销，已扣回 ${data.refundPoints} 分`
    )
    revokeVisible.value = false
    await load()
  } finally {
    revoking.value = false
  }
}

onMounted(async () => {
  const [recyclerData, categoryData] = await Promise.all([
    listRecyclers({ page: 1, size: 50 }),
    listCategories()
  ])
  recyclers.value = recyclerData.list
  categories.value = categoryData
  await load()
})
</script>

<style scoped>
.summary-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  background: var(--line);
  border-bottom: 1px solid var(--line);
}

.summary-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding: 14px 20px;
  background: #fbfdfb;
}

.summary-item span {
  color: var(--ink-500);
  font-size: 11.5px;
}

.summary-item strong {
  margin-left: auto;
  color: var(--ink-900);
  font-size: 18px;
  font-weight: 650;
}

.summary-item small {
  color: var(--ink-400);
  font-size: 10.5px;
}

.flag-list {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.flag-tag {
  margin-right: 4px;
}

.detail-note {
  margin-top: 18px;
}

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

@media (max-width: 1100px) {
  .summary-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
