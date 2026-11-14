<template>
  <div class="page">
    <PageHeader
      title="申诉处理"
      description="处理用户对投递或兑换结果的申诉。处理结论需写明依据；需要补偿积分时另走「积分调整」。"
    >
      <template #actions>
        <el-button :icon="Coin" @click="router.push('/audit/points')">积分调整</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="reloadAll">刷新</el-button>
      </template>
    </PageHeader>

    <section class="panel">
      <div class="status-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          class="status-tab"
          :class="{ active: activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
          <span v-if="tab.key === 'PENDING' && pendingCount" class="status-tab__badge">{{ pendingCount }}</span>
        </button>
      </div>

      <div class="table-wrap">
        <el-table :data="appeals" v-loading="loading" empty-text="当前分类下没有申诉">
          <el-table-column label="提交时间" width="150">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="用户" width="115">
            <template #default="{ row }">
              <div class="cell-title">{{ row.userNickname }}</div>
              <div class="cell-sub mono">{{ row.userPhoneMasked }}</div>
            </template>
          </el-table-column>
          <el-table-column label="关联对象" width="170">
            <template #default="{ row }">
              <div>{{ APPEAL_TARGET[row.targetType] || row.targetType }}</div>
              <div class="cell-sub mono">{{ row.targetNo }}</div>
            </template>
          </el-table-column>
          <el-table-column label="问题描述" min-width="200">
            <template #default="{ row }">
              <el-tooltip :content="row.description" placement="top" :show-after="300">
                <span class="desc-cell">{{ truncate(row.description, 30) }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }"><StatusTag :value="row.status" kind="appeal" /></template>
          </el-table-column>
          <el-table-column label="处理信息" width="150">
            <template #default="{ row }">
              <template v-if="row.handledAt">
                <div>{{ row.handledBy || '—' }}</div>
                <div class="cell-sub">{{ formatDateTime(row.handledAt) }}</div>
              </template>
              <span v-else class="text-warn">待处理</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" align="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">查看</el-button>
              <el-button v-if="row.status === 'PENDING'" link type="warning" @click="openHandle(row)">处理</el-button>
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
        处理申诉本身不直接改积分；采纳后如需补偿，请通过「积分调整」操作，保证每笔积分变动都有独立明细。
      </div>
    </section>

    <el-drawer v-model="detailVisible" title="申诉详情" size="500px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="提交用户">
            {{ detail.userNickname }} · {{ detail.userPhoneMasked }}
          </el-descriptions-item>
          <el-descriptions-item label="关联对象">
            {{ APPEAL_TARGET[detail.targetType] || detail.targetType }}
            <span class="mono"> {{ detail.targetNo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="状态"><StatusTag :value="detail.status" kind="appeal" /></el-descriptions-item>
          <el-descriptions-item label="问题描述">
            <p class="desc-block">{{ detail.description }}</p>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.conclusion" label="处理结论">
            <p class="desc-block">{{ detail.conclusion }}</p>
            <span class="muted">{{ detail.handledBy }} · {{ formatDateTime(detail.handledAt) }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.status === 'PENDING'" class="drawer-actions">
          <el-button type="primary" @click="openHandle(detail)">立即处理</el-button>
        </div>
        <el-alert
          v-else-if="detail.status === 'ACCEPTED'"
          type="success"
          :closable="false"
          show-icon
          title="该申诉已采纳"
          description="如需补偿积分，请前往「积分调整」为对应用户补分。"
          class="drawer-note"
        />
      </template>
    </el-drawer>

    <el-dialog v-model="handleVisible" title="处理申诉" width="520px" align-center>
      <div v-if="handleTarget" class="appeal-brief">
        <div class="appeal-brief__head">
          <strong>{{ handleTarget.userNickname }}</strong>
          <span class="mono faint">{{ handleTarget.targetNo }}</span>
          <StatusTag :value="handleTarget.status" kind="appeal" />
        </div>
        <p>{{ handleTarget.description }}</p>
      </div>

      <el-form label-position="top">
        <el-form-item label="处理结果">
          <el-radio-group v-model="handleForm.result">
            <el-radio-button value="ACCEPT">采纳申诉</el-radio-button>
            <el-radio-button value="REJECT">驳回申诉</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理结论（10–500 字）" required>
          <el-input
            v-model="handleForm.conclusion"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="写明核实过程、依据与处理结果；如补偿积分，请注明补偿明细号"
          />
        </el-form-item>
      </el-form>

      <el-alert
        v-if="handleForm.result === 'ACCEPT'"
        type="info"
        :closable="false"
        show-icon
        title="采纳后如需补偿积分"
        description="处理申诉接口不直接修改积分。请提交结论后，到「积分调整」为用户补分并记录明细。"
      />
      <el-alert
        v-else
        type="warning"
        :closable="false"
        show-icon
        title="驳回将向用户展示处理结论"
        description="请说明核实结果，避免只写「不符合条件」。"
      />

      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button v-if="handleForm.result === 'ACCEPT'" @click="goAdjust">先不提交，去调整积分</el-button>
        <el-button type="primary" :loading="handling" @click="submitHandle">提交处理结果</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Coin, InfoFilled, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { handleAppeal, listAppeals } from '@/api/account'
import { APPEAL_TARGET } from '@/utils/dict'
import { formatDateTime, truncate } from '@/utils/format'

const router = useRouter()

const tabs = [
  { key: 'PENDING', label: '待处理' },
  { key: 'ACCEPTED', label: '已采纳' },
  { key: 'REJECTED', label: '已驳回' },
  { key: '', label: '全部' }
]

const activeTab = ref('PENDING')
const appeals = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const pendingCount = ref(0)

const detailVisible = ref(false)
const detail = ref(null)

const handleVisible = ref(false)
const handleTarget = ref(null)
const handling = ref(false)
const handleForm = reactive({ result: 'ACCEPT', conclusion: '' })

async function load() {
  loading.value = true
  try {
    const data = await listAppeals({
      page: page.value,
      size: 20,
      status: activeTab.value || undefined
    })
    appeals.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadPendingCount() {
  const data = await listAppeals({ page: 1, size: 1, status: 'PENDING' })
  pendingCount.value = data.total
}

async function reloadAll() {
  await Promise.all([load(), loadPendingCount()])
}

function switchTab(key) {
  activeTab.value = key
  page.value = 1
  load()
}

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

function openHandle(row) {
  handleTarget.value = row
  handleForm.result = 'ACCEPT'
  handleForm.conclusion = ''
  handleVisible.value = true
}

async function submitHandle() {
  const conclusion = handleForm.conclusion.trim()
  if (conclusion.length < 10 || conclusion.length > 500) {
    ElMessage.warning('处理结论需为 10–500 字')
    return
  }
  handling.value = true
  try {
    const data = await handleAppeal(handleTarget.value.appealId, {
      result: handleForm.result,
      conclusion
    })
    ElMessage.success(data.status === 'ACCEPTED' ? '申诉已采纳' : '申诉已驳回')
    handleVisible.value = false
    detailVisible.value = false
    await reloadAll()
  } finally {
    handling.value = false
  }
}

function goAdjust() {
  const target = handleTarget.value
  handleVisible.value = false
  detailVisible.value = false
  router.push({ path: '/audit/points', query: { userId: target.userId } })
}

onMounted(reloadAll)
</script>

<style scoped>
.status-tabs {
  display: flex;
  gap: 4px;
  padding: 12px 18px 0;
  border-bottom: 1px solid var(--line);
  flex-wrap: wrap;
}

.status-tab {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 10px 14px 12px;
  border: 0;
  background: transparent;
  color: var(--ink-500);
  font-family: inherit;
  font-size: 13px;
  font-weight: 550;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: color 0.16s ease, border-color 0.16s ease;
}

.status-tab:hover {
  color: var(--ink-900);
}

.status-tab.active {
  color: var(--brand-700);
  border-bottom-color: var(--brand-600);
}

.status-tab__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--amber-50);
  color: var(--amber-700);
  font-size: 10px;
  font-weight: 700;
}

.desc-cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ink-700);
}

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

.desc-block {
  white-space: pre-wrap;
  line-height: 1.7;
}

.drawer-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.drawer-note {
  margin-top: 18px;
}

.appeal-brief {
  padding: 14px 16px;
  margin-bottom: 18px;
  border-radius: 11px;
  background: #fbfdfb;
  border: 1px solid var(--line);
}

.appeal-brief__head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 7px;
}

.appeal-brief__head strong {
  font-size: 13.5px;
  font-weight: 600;
}

.appeal-brief p {
  color: var(--ink-700);
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
}
</style>
