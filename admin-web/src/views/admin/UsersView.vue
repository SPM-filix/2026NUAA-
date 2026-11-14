<template>
  <div class="page">
    <PageHeader
      title="用户账号"
      description="查询用户、禁用或启用账号、重置密码。管理员不能直接设置用户密码，只能生成一次性临时密码。"
    >
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <section class="panel">
      <div class="toolbar">
        <div class="toolbar__filters">
          <el-input
            v-model="query.keyword"
            placeholder="搜索昵称 / 编号 / 手机号"
            clearable
            :prefix-icon="Search"
            style="width: 250px"
            @keyup.enter="search"
          />
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="search">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="已禁用" value="DISABLED" />
          </el-select>
          <el-button type="primary" plain @click="search">查询</el-button>
          <el-button text @click="resetQuery">重置</el-button>
        </div>
        <span class="muted">共 {{ total }} 个用户</span>
      </div>

      <div class="table-wrap">
        <el-table :data="users" v-loading="loading" empty-text="没有符合条件的用户">
          <el-table-column label="用户" min-width="190">
            <template #default="{ row }">
              <div class="user-cell">
                <span class="avatar-initial">{{ row.nickname.slice(0, 1) }}</span>
                <div>
                  <div class="cell-title">{{ row.nickname }}</div>
                  <div class="cell-sub mono">编号 {{ row.userCode }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="手机号" width="150">
            <template #default="{ row }"><span class="mono">{{ row.phoneMasked }}</span></template>
          </el-table-column>
          <el-table-column label="积分余额" width="110" align="right">
            <template #default="{ row }">
              <span class="points num">{{ formatNumber(row.pointsBalance) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="业务数据" width="150">
            <template #default="{ row }">
              <span class="muted">投递 {{ row.deliveryCount }} · 兑换 {{ row.orderCount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }"><StatusTag :value="row.status" kind="user" /></template>
          </el-table-column>
          <el-table-column label="注册时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="right">
            <template #default="{ row }">
              <el-button
                link
                :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
                @click="row.status === 'ACTIVE' ? openDisable(row) : enable(row)"
              >
                {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
              </el-button>
              <el-button link type="primary" @click="resetPassword(row)">重置密码</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="prev, pager, next, total"
          background
          @current-change="load"
        />
      </div>

      <div class="panel-note">
        <el-icon><InfoFilled /></el-icon>
        禁用后该用户所有接口返回 2002，已生成的待核销订单不受影响；重置密码使用户首次登录后必须改密。
      </div>
    </section>

    <el-dialog v-model="disableVisible" title="禁用用户" width="460px" align-center>
      <div class="dialog-user">
        <span class="avatar-initial">{{ disableTarget?.nickname?.slice(0, 1) }}</span>
        <div>
          <strong>{{ disableTarget?.nickname }}</strong>
          <p class="muted">{{ disableTarget?.userCode }} · {{ disableTarget?.phoneMasked }}</p>
        </div>
      </div>
      <el-form label-position="top">
        <el-form-item label="禁用原因（必填，≤200 字）">
          <el-input
            v-model="disableReason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="记录禁用原因，便于后续审计"
          />
        </el-form-item>
      </el-form>
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="禁用后该用户不能登录或调用任何接口"
      />
      <template #footer>
        <el-button @click="disableVisible = false">取消</el-button>
        <el-button type="danger" :loading="disabling" @click="confirmDisable">确认禁用</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="临时密码已生成" width="460px" align-center>
      <div class="temp-password">
        <span class="temp-password__label">一次性临时密码</span>
        <div class="temp-password__value">
          <code class="mono">{{ resetResult?.tempPassword }}</code>
          <el-button size="small" :icon="CopyDocument" @click="copyPassword">复制</el-button>
        </div>
        <p>有效期至 {{ formatDateTime(resetResult?.expireAt) }}，关闭弹窗后不可再次查看。</p>
      </div>
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="请通过线下安全渠道告知用户"
        description="用户使用临时密码登录后，必须先修改密码才能访问其他页面。"
      />
      <template #footer>
        <el-button type="primary" @click="resetVisible = false">我已记录，关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument, InfoFilled, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { listUsers, resetUserPassword, setUserStatus } from '@/api/account'
import { formatDateTime, formatNumber } from '@/utils/format'

const users = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', status: '', page: 1, size: 20 })

const disableVisible = ref(false)
const disableTarget = ref(null)
const disableReason = ref('')
const disabling = ref(false)

const resetVisible = ref(false)
const resetResult = ref(null)
const resetTarget = ref(null)

async function load() {
  loading.value = true
  try {
    const data = await listUsers({
      page: query.page,
      size: query.size,
      keyword: query.keyword.trim() || undefined,
      status: query.status || undefined
    })
    users.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function resetQuery() {
  query.keyword = ''
  query.status = ''
  search()
}

function openDisable(row) {
  disableTarget.value = row
  disableReason.value = ''
  disableVisible.value = true
}

async function confirmDisable() {
  const reason = disableReason.value.trim()
  if (!reason) {
    ElMessage.warning('禁用用户必须填写原因')
    return
  }
  disabling.value = true
  try {
    await setUserStatus(disableTarget.value.userId, { status: 'DISABLED', reason })
    ElMessage.success(`已禁用「${disableTarget.value.nickname}」`)
    disableVisible.value = false
    await load()
  } finally {
    disabling.value = false
  }
}

async function enable(row) {
  try {
    await ElMessageBox.confirm(
      `确认恢复「${row.nickname}」的账号？恢复后该用户可正常登录并继续使用积分。`,
      '启用账号',
      { confirmButtonText: '启用', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await setUserStatus(row.userId, { status: 'ACTIVE' })
  ElMessage.success(`已启用「${row.nickname}」`)
  await load()
}

async function resetPassword(row) {
  try {
    await ElMessageBox.confirm(
      `确认重置「${row.nickname}」的密码？系统将生成一次性临时密码，用户首次登录后必须修改。`,
      '重置密码',
      { confirmButtonText: '重置', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  const data = await resetUserPassword(row.userId)
  resetTarget.value = row
  resetResult.value = data
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

onMounted(load)
</script>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.points {
  color: var(--brand-700);
  font-size: 14px;
  font-weight: 650;
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

.dialog-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 16px;
  border-radius: 10px;
  background: #f8faf9;
  border: 1px solid var(--line);
}

.dialog-user strong {
  font-size: 13.5px;
  font-weight: 600;
}

.dialog-user p {
  margin-top: 1px;
  font-size: 11.5px;
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
