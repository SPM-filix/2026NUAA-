<template>
  <div class="page">
    <PageHeader
      title="积分调整"
      description="人工补分或扣分的唯一入口，用于违规积分撤销与申诉补偿。每次调整都会写入积分明细并记录操作人。"
    >
      <template #actions>
        <el-button :icon="ChatDotRound" @click="router.push('/audit/appeals')">去处理申诉</el-button>
      </template>
    </PageHeader>

    <div class="points-grid">
      <section class="panel form-panel">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">发起积分调整</h3>
            <p class="panel-sub">先选择用户，再填写调整值与原因</p>
          </div>
        </div>

        <div class="form-body">
          <label class="field-label">目标用户</label>
          <el-select
            v-model="selectedId"
            filterable
            remote
            clearable
            :remote-method="searchUsers"
            :loading="searching"
            placeholder="输入昵称 / 用户编号 / 手机号搜索"
            style="width: 100%"
            @change="onSelect"
          >
            <el-option
              v-for="user in options"
              :key="user.userId"
              :label="`${user.nickname} · ${user.userCode} · ${user.phoneMasked}`"
              :value="user.userId"
            >
              <div class="option-row">
                <span>{{ user.nickname }}</span>
                <span class="mono faint">{{ user.userCode }} · {{ user.phoneMasked }}</span>
                <span class="num option-points">{{ user.pointsBalance }} 分</span>
              </div>
            </el-option>
          </el-select>

          <div v-if="selectedUser" class="user-card">
            <span class="avatar-initial">{{ selectedUser.nickname.slice(0, 1) }}</span>
            <div class="user-card__meta">
              <strong>{{ selectedUser.nickname }}</strong>
              <small class="mono">{{ selectedUser.userCode }} · {{ selectedUser.phoneMasked }}</small>
            </div>
            <StatusTag :value="selectedUser.status" kind="user" />
            <div class="user-card__balance">
              <span>当前余额</span>
              <strong class="num">{{ formatNumber(selectedUser.pointsBalance) }}<small>分</small></strong>
            </div>
          </div>
          <p v-else class="field-hint">仅能调整状态正常的用户；禁用用户需先恢复账号。</p>

          <el-form label-position="top" class="adjust-form">
            <el-form-item label="调整值（正数补分，负数扣分）">
              <el-input-number
                v-model="change"
                :min="-100000"
                :max="100000"
                :step="10"
                :precision="0"
                controls-position="right"
                style="width: 220px"
              />
              <span class="field-hint inline">绝对值 ≤ 100000，不能为 0</span>
            </el-form-item>
            <el-form-item label="调整原因（10–200 字）" required>
              <el-input
                v-model="reason"
                type="textarea"
                :rows="3"
                maxlength="200"
                show-word-limit
                placeholder="写明依据，如：重复投递违规积分撤销 / 申诉工单 A-001 补偿"
              />
            </el-form-item>
          </el-form>

          <div class="preview-card" :class="{ 'is-invalid': previewBalance < 0 }">
            <div>
              <span>调整后余额</span>
              <strong class="num">{{ selectedUser ? formatNumber(previewBalance) : '—' }}<small>分</small></strong>
            </div>
            <p v-if="previewBalance < 0" class="text-danger">
              扣减后余额会变为负数，接口将返回 3011，请减少扣减数量。
            </p>
            <p v-else>调整会立即生效，并写入一条「人工调整」积分明细。</p>
          </div>

          <div class="submit-row">
            <el-button
              type="primary"
              size="large"
              :loading="submitting"
              :disabled="!selectedUser || previewBalance < 0"
              @click="submit"
            >
              提交调整
            </el-button>
            <el-button size="large" @click="resetForm">清空</el-button>
          </div>
        </div>
      </section>

      <aside class="side">
        <section v-if="lastResult" class="panel result-panel">
          <div class="panel-head">
            <div>
              <h3 class="panel-title">最近一次调整</h3>
              <p class="panel-sub">明细号 {{ lastResult.logId }}</p>
            </div>
          </div>
          <div class="result-body">
            <div class="result-user">{{ lastResult.nickname }} · {{ lastResult.userCode }}</div>
            <div class="result-balance">
              <span>{{ formatNumber(lastResult.balanceBefore) }}</span>
              <el-icon><Right /></el-icon>
              <strong class="num">{{ formatNumber(lastResult.balanceAfter) }}</strong>
              <span class="result-change" :class="lastResult.change > 0 ? 'amount-pos' : 'amount-neg'">
                {{ lastResult.change > 0 ? '+' : '' }}{{ lastResult.change }} 分
              </span>
            </div>
            <el-alert type="success" :closable="false" show-icon title="调整已生效并写入明细" />
          </div>
        </section>

        <section class="panel guide-panel">
          <div class="panel-head">
            <div>
              <h3 class="panel-title">使用规则</h3>
              <p class="panel-sub">积分只能通过以下方式产生或变动</p>
            </div>
          </div>
          <div class="guide-body">
            <div class="guide-item">
              <span class="guide-icon"><el-icon><WarningFilled /></el-icon></span>
              <div>
                <strong>违规积分撤销</strong>
                <p>核查流水后发现重复录入、异常重量等问题，先撤销流水，再视情况人工扣分。</p>
              </div>
            </div>
            <div class="guide-item">
              <span class="guide-icon"><el-icon><CircleCheck /></el-icon></span>
              <div>
                <strong>申诉补偿</strong>
                <p>申诉采纳后需要补偿时，另在本页操作，处理申诉接口本身不改积分。</p>
              </div>
            </div>
            <div class="guide-item">
              <span class="guide-icon"><el-icon><Lock /></el-icon></span>
              <div>
                <strong>不可越过的边界</strong>
                <p>不能把余额扣成负数；管理员不直接参与称重与核销，避免自录自发。</p>
              </div>
            </div>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { adjustPoints, listUsers } from '@/api/account'
import { formatNumber } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const options = ref([])
const selectedId = ref(null)
const searching = ref(false)
const change = ref(0)
const reason = ref('')
const submitting = ref(false)
const lastResult = ref(null)

const selectedUser = computed(() => options.value.find((user) => user.userId === selectedId.value) || null)
const previewBalance = computed(() =>
  selectedUser.value ? selectedUser.value.pointsBalance + (Number(change.value) || 0) : 0
)

async function searchUsers(keyword = '') {
  searching.value = true
  try {
    const data = await listUsers({ page: 1, size: 20, keyword: keyword || undefined })
    options.value = data.list
  } finally {
    searching.value = false
  }
}

function onSelect() {
  lastResult.value = null
}

function resetForm() {
  selectedId.value = null
  change.value = 0
  reason.value = ''
}

async function submit() {
  if (!selectedUser.value) {
    ElMessage.warning('请先选择目标用户')
    return
  }
  if (!Number.isInteger(change.value) || change.value === 0) {
    ElMessage.warning('调整值需为非 0 整数')
    return
  }
  const text = reason.value.trim()
  if (text.length < 10 || text.length > 200) {
    ElMessage.warning('调整原因需为 10–200 字')
    return
  }
  if (previewBalance.value < 0) {
    ElMessage.warning('调整后余额不能为负')
    return
  }

  const action = change.value > 0 ? '增加' : '扣减'
  try {
    await ElMessageBox.confirm(
      `确认给「${selectedUser.value.nickname}」${action} ${Math.abs(change.value)} 分？调整会立即生效并写入积分明细。`,
      '确认积分调整',
      { confirmButtonText: '确认调整', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    const data = await adjustPoints({
      userId: selectedUser.value.userId,
      change: change.value,
      reason: text
    })
    lastResult.value = {
      ...data,
      nickname: selectedUser.value.nickname,
      userCode: selectedUser.value.userCode
    }
    selectedUser.value.pointsBalance = data.balanceAfter
    ElMessage.success('积分调整成功')
    change.value = 0
    reason.value = ''
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await searchUsers()
  const preset = Number(route.query.userId)
  if (preset && options.value.some((user) => user.userId === preset)) {
    selectedId.value = preset
  }
})
</script>

<style scoped>
.points-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.7fr);
  gap: 20px;
  align-items: start;
}

.form-body {
  padding: 22px 24px 26px;
}

.field-label {
  display: block;
  margin-bottom: 6px;
  color: var(--ink-700);
  font-size: 13px;
  font-weight: 550;
}

.field-hint {
  display: block;
  margin-top: 6px;
  color: var(--ink-400);
  font-size: 11px;
}

.field-hint.inline {
  display: inline-block;
  margin: 0 0 0 12px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.option-points {
  margin-left: auto;
  color: var(--brand-700);
  font-weight: 600;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fbfdfb;
}

.user-card__meta {
  flex: 1;
  min-width: 0;
}

.user-card__meta strong {
  display: block;
  font-size: 14px;
  font-weight: 600;
}

.user-card__meta small {
  display: block;
  color: var(--ink-500);
  font-size: 11px;
}

.user-card__balance {
  text-align: right;
}

.user-card__balance span {
  display: block;
  color: var(--ink-500);
  font-size: 10.5px;
}

.user-card__balance strong {
  color: var(--brand-700);
  font-size: 19px;
  font-weight: 650;
}

.user-card__balance small {
  margin-left: 2px;
  color: var(--ink-400);
  font-size: 10px;
  font-weight: 500;
}

.adjust-form {
  margin-top: 20px;
}

.preview-card {
  padding: 16px 18px;
  border-radius: 12px;
  background: var(--brand-50);
  border: 1px solid #dceee4;
}

.preview-card.is-invalid {
  background: var(--red-50);
  border-color: #f3cfcb;
}

.preview-card > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.preview-card span {
  color: var(--ink-500);
  font-size: 12px;
}

.preview-card strong {
  color: var(--ink-900);
  font-size: 24px;
  font-weight: 650;
}

.preview-card strong small {
  margin-left: 3px;
  color: var(--ink-400);
  font-size: 10.5px;
  font-weight: 500;
}

.preview-card p {
  margin-top: 6px;
  color: var(--ink-500);
  font-size: 11px;
}

.submit-row {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.submit-row .el-button {
  height: 46px;
  min-width: 140px;
}

.side {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.result-body {
  padding: 18px 22px 22px;
}

.result-user {
  color: var(--ink-500);
  font-size: 12px;
  margin-bottom: 10px;
}

.result-balance {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  font-size: 17px;
  color: var(--ink-400);
}

.result-balance strong {
  color: var(--brand-700);
  font-size: 22px;
  font-weight: 650;
}

.result-change {
  margin-left: auto;
  font-size: 13px;
}

.guide-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px 22px 22px;
}

.guide-item {
  display: flex;
  gap: 12px;
}

.guide-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--brand-100);
  color: var(--brand-700);
  font-size: 15px;
  flex: none;
}

.guide-item strong {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
}

.guide-item p {
  margin-top: 3px;
  color: var(--ink-500);
  font-size: 11.5px;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .points-grid {
    grid-template-columns: 1fr;
  }
}
</style>
