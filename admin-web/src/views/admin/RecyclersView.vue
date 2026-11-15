<template>
  <div class="page">
    <PageHeader
      title="回收员审核"
      description="审核回收员注册申请、分配站点、禁用账号或重置密码。仅回收员可进行称重登记与核销。"
    >
      <template #actions>
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

      <div class="toolbar">
        <div class="toolbar__filters">
          <el-input
            v-model="keyword"
            placeholder="搜索姓名 / 手机号 / 工号"
            clearable
            :prefix-icon="Search"
            style="width: 250px"
            @keyup.enter="search"
          />
          <el-button type="primary" plain @click="search">查询</el-button>
          <el-button text @click="resetKeyword">重置</el-button>
        </div>
        <span class="muted">共 {{ total }} 人</span>
      </div>

      <div class="table-wrap">
        <el-table :data="recyclers" v-loading="loading" empty-text="当前分类下没有回收员">
          <el-table-column label="回收员" width="150">
            <template #default="{ row }">
              <div class="user-cell">
                <span class="avatar-initial">{{ row.name.slice(0, 1) }}</span>
                <div>
                  <div class="cell-title">{{ row.name }}</div>
                  <div class="cell-sub mono">工号 {{ row.employeeNo }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="手机号" width="130">
            <template #default="{ row }"><span class="mono">{{ row.phone }}</span></template>
          </el-table-column>
          <el-table-column label="所属站点" width="125">
            <template #default="{ row }">{{ row.siteName || '未分配' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <StatusTag :value="row.status" kind="recycler" />
              <div v-if="row.status === 'REJECTED' && row.rejectReason" class="cell-sub text-danger">
                {{ truncate(row.rejectReason, 14) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="申请说明" min-width="150">
            <template #default="{ row }">
              <el-tooltip :content="row.applyReason || '—'" placement="top" :show-after="300">
                <span class="apply-reason">{{ truncate(row.applyReason || '—', 18) }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="申请时间" width="150">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="right">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <el-button link type="success" @click="openApprove(row)">通过</el-button>
                <el-button link type="danger" @click="openReject(row)">驳回</el-button>
              </template>
              <template v-else-if="row.status === 'APPROVED'">
                <el-button link type="warning" @click="disable(row)">禁用</el-button>
                <el-button link type="primary" @click="resetPassword(row, 'recycler')">重置密码</el-button>
              </template>
              <template v-else-if="row.status === 'DISABLED'">
                <el-button link type="success" @click="enable(row)">启用</el-button>
                <el-button link type="primary" @click="resetPassword(row, 'recycler')">重置密码</el-button>
              </template>
              <span v-else class="faint">—</span>
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
        申请通过时必须分配启用中的站点；驳回需填写不少于 10 字的原因，用户可修改后重新申请。
      </div>
    </section>

    <!-- 通过审核 -->
    <el-dialog v-model="approveVisible" title="通过回收员申请" width="480px" align-center>
      <div class="applicant">
        <span class="avatar-initial">{{ approveTarget?.name?.slice(0, 1) }}</span>
        <div>
          <strong>{{ approveTarget?.name }}</strong>
          <p class="muted mono">{{ approveTarget?.employeeNo }} · {{ approveTarget?.phone }}</p>
        </div>
      </div>
      <p v-if="approveTarget?.applyReason" class="apply-box">{{ approveTarget.applyReason }}</p>

      <el-form label-position="top">
        <el-form-item label="分配站点" required>
          <el-select v-model="approveSiteId" placeholder="请选择启用中的站点" style="width: 100%">
            <el-option
              v-for="site in enabledSites"
              :key="site.siteId"
              :label="`${site.name} · ${site.address}`"
              :value="site.siteId"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="通过后回收员即可登录并开始登记投递"
        description="回收员只能看到本人经手的流水，不能修改积分规则或管理商品。"
      />

      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="approving" @click="submitApprove">通过并分配</el-button>
      </template>
    </el-dialog>

    <!-- 驳回 -->
    <el-dialog v-model="rejectVisible" title="驳回回收员申请" width="480px" align-center>
      <div class="applicant">
        <span class="avatar-initial">{{ rejectTarget?.name?.slice(0, 1) }}</span>
        <div>
          <strong>{{ rejectTarget?.name }}</strong>
          <p class="muted mono">{{ rejectTarget?.employeeNo }} · {{ rejectTarget?.phone }}</p>
        </div>
      </div>

      <el-form label-position="top">
        <el-form-item label="驳回原因（10–200 字）" required>
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="说明驳回原因，便于申请人修改后重新提交"
          />
        </el-form-item>
      </el-form>

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="被驳回的账号不能登录"
        description="登录时返回 2004，并展示驳回原因；申请人可以重新提交申请。"
      />

      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 临时密码 -->
    <el-dialog v-model="resetVisible" title="临时密码已生成" width="460px" align-center>
      <div class="temp-password">
        <span class="temp-password__label">一次性临时密码</span>
        <div class="temp-password__value">
          <code class="mono">{{ resetResult?.tempPassword }}</code>
          <el-button size="small" :icon="CopyDocument" @click="copyPassword">复制</el-button>
        </div>
        <p>有效期至 {{ formatDateTime(resetResult?.expireAt) }}，关闭后不可再次查看。</p>
      </div>
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="回收员首次登录后必须修改密码"
        description="请通过线下安全渠道告知本人，不要发在群里。"
      />
      <template #footer>
        <el-button type="primary" @click="resetVisible = false">我已记录，关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument, InfoFilled, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  approveRecycler,
  listRecyclers,
  rejectRecycler,
  resetRecyclerPassword,
  setRecyclerStatus
} from '@/api/account'
import { listSites } from '@/api/site'
import { formatDateTime, truncate } from '@/utils/format'

const tabs = [
  { key: 'PENDING', label: '待审核' },
  { key: 'APPROVED', label: '已通过' },
  { key: 'REJECTED', label: '已驳回' },
  { key: 'DISABLED', label: '已禁用' },
  { key: '', label: '全部' }
]

const activeTab = ref('PENDING')
const keyword = ref('')
const recyclers = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const pendingCount = ref(0)

const approveVisible = ref(false)
const approveTarget = ref(null)
const approveSiteId = ref(null)
const approving = ref(false)
const enabledSites = ref([])

const rejectVisible = ref(false)
const rejectTarget = ref(null)
const rejectReason = ref('')
const rejecting = ref(false)

const resetVisible = ref(false)
const resetResult = ref(null)

async function load() {
  loading.value = true
  try {
    const data = await listRecyclers({
      page: page.value,
      size: 20,
      status: activeTab.value || undefined,
      keyword: keyword.value.trim() || undefined
    })
    recyclers.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadPendingCount() {
  const data = await listRecyclers({ page: 1, size: 1, status: 'PENDING' })
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

function search() {
  page.value = 1
  load()
}

function resetKeyword() {
  keyword.value = ''
  search()
}

async function ensureSites() {
  if (!enabledSites.value.length) {
    enabledSites.value = await listSites({ enabledOnly: true })
  }
}

async function openApprove(row) {
  approveTarget.value = row
  approveSiteId.value = null
  await ensureSites()
  approveVisible.value = true
}

async function submitApprove() {
  if (!approveSiteId.value) {
    ElMessage.warning('请选择分配站点')
    return
  }
  approving.value = true
  try {
    await approveRecycler(approveTarget.value.recyclerId, { siteId: approveSiteId.value })
    ElMessage.success(`已通过「${approveTarget.value.name}」的申请`)
    approveVisible.value = false
    await reloadAll()
  } finally {
    approving.value = false
  }
}

function openReject(row) {
  rejectTarget.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function submitReject() {
  const reason = rejectReason.value.trim()
  if (reason.length < 10 || reason.length > 200) {
    ElMessage.warning('驳回原因需为 10–200 字')
    return
  }
  rejecting.value = true
  try {
    await rejectRecycler(rejectTarget.value.recyclerId, { reason })
    ElMessage.success(`已驳回「${rejectTarget.value.name}」的申请`)
    rejectVisible.value = false
    await reloadAll()
  } finally {
    rejecting.value = false
  }
}

async function disable(row) {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('可填写禁用原因（选填），将记录在账号信息中。', '禁用回收员', {
      confirmButtonText: '禁用',
      cancelButtonText: '取消',
      inputPlaceholder: '禁用原因（选填）',
      inputValue: '',
      type: 'warning'
    })
    reason = result.value || ''
  } catch {
    return
  }
  await setRecyclerStatus(row.recyclerId, { status: 'DISABLED', reason: reason.trim() || null })
  ElMessage.success(`已禁用「${row.name}」`)
  await reloadAll()
}

async function enable(row) {
  try {
    await ElMessageBox.confirm(
      `确认恢复「${row.name}」的回收员账号？恢复后即可继续登记投递与核销。`,
      '启用回收员',
      { confirmButtonText: '启用', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await setRecyclerStatus(row.recyclerId, { status: 'APPROVED' })
  ElMessage.success(`已启用「${row.name}」`)
  await reloadAll()
}

async function resetPassword(row) {
  try {
    await ElMessageBox.confirm(
      `确认重置「${row.name}」的密码？系统将生成一次性临时密码，首次登录后必须修改。`,
      '重置回收员密码',
      { confirmButtonText: '重置', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  resetResult.value = await resetRecyclerPassword(row.recyclerId)
  resetVisible.value = true
}

async function copyPassword() {
  try {
    await navigator.clipboard.writeText(resetResult.value.tempPassword)
    ElMessage.success('临时密码已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选中复制')
  }
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
  position: relative;
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

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.apply-reason {
  display: inline-block;
  max-width: 100%;
  color: var(--ink-700);
  font-size: 12.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.applicant {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 14px;
  border-radius: 10px;
  background: #f8faf9;
  border: 1px solid var(--line);
}

.applicant strong {
  font-size: 13.5px;
  font-weight: 600;
}

.applicant p {
  margin-top: 1px;
  font-size: 11.5px;
}

.apply-box {
  padding: 10px 14px;
  margin-bottom: 16px;
  border-left: 3px solid var(--brand-500);
  border-radius: 0 8px 8px 0;
  background: var(--brand-50);
  color: var(--ink-700);
  font-size: 12px;
  line-height: 1.7;
}

.temp-password {
  padding: 18px;
  border-radius: 12px;
  background: #f8faf9;
  border: 1px solid var(--line);
  margin-bottom: 16px;
}

.temp-password__label {
  color: var(--ink-500);
  font-size: 11.5px;
}

.temp-password__value {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 8px 0;
  padding: 12px 14px;
  border-radius: 9px;
  background: #fff;
  border: 1px dashed var(--line-strong);
}

.temp-password code {
  color: var(--brand-700);
  font-size: 20px;
  font-weight: 650;
  letter-spacing: 0.12em;
}

.temp-password p {
  color: var(--ink-400);
  font-size: 11px;
}
</style>
