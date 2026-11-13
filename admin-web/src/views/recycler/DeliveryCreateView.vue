<template>
  <div class="page">
    <PageHeader
      title="投递登记"
      description="选择垃圾类别、输入重量，系统按当前积分规则自动计算并发放积分。"
    >
      <template #actions>
        <el-button :icon="Aim" @click="router.push('/workbench/identify')">识别新用户</el-button>
        <el-button :icon="List" @click="router.push('/workbench/records')">我的流水</el-button>
      </template>
    </PageHeader>

    <section v-if="!currentUser" class="panel empty-panel">
      <span class="empty-panel__icon"><el-icon><User /></el-icon></span>
      <h2>还没有选择用户</h2>
      <p>投递登记前需先识别用户身份，避免把积分发到错误账号。</p>
      <el-button type="primary" size="large" :icon="Aim" @click="router.push('/workbench/identify')">
        去识别用户
      </el-button>
    </section>

    <template v-else>
      <div class="delivery-grid">
        <section class="panel form-panel">
          <div class="user-bar">
            <span class="avatar-initial user-bar__avatar">{{ currentUser.nickname.slice(0, 1) }}</span>
            <div class="user-bar__meta">
              <strong>{{ currentUser.nickname }}</strong>
              <small class="mono">{{ currentUser.userCode }} · {{ currentUser.phoneMasked }}</small>
            </div>
            <span class="user-bar__balance">当前余额 <strong class="num">{{ formatNumber(currentUser.pointsBalance) }}</strong> 分</span>
            <el-button text type="primary" @click="router.push('/workbench/identify')">更换用户</el-button>
          </div>

          <div class="form-body">
            <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
              <el-form-item label="垃圾类别" prop="categoryId">
                <div class="category-grid">
                  <button
                    v-for="category in categories"
                    :key="category.categoryId"
                    type="button"
                    class="category-card"
                    :class="{ active: form.categoryId === category.categoryId }"
                    @click="form.categoryId = category.categoryId"
                  >
                    <span class="category-card__top">
                      <span class="category-card__name">{{ category.name }}</span>
                      <el-icon v-if="form.categoryId === category.categoryId" class="category-card__check">
                        <Select />
                      </el-icon>
                    </span>
                    <span class="category-card__code mono">{{ category.code }}</span>
                    <span class="category-card__rate num">{{ category.pointsPerKg }}<small>分 / kg</small></span>
                  </button>
                </div>
              </el-form-item>

              <el-form-item label="重量（kg）" prop="weight">
                <div class="weight-row">
                  <el-input-number
                    v-model="form.weight"
                    :min="0.01"
                    :max="100"
                    :step="0.1"
                    :precision="2"
                    size="large"
                    controls-position="right"
                    placeholder="0.00"
                  />
                  <span class="weight-hint">允许 0.01–100 kg，最多两位小数</span>
                </div>
              </el-form-item>

              <el-form-item label="备注（可选）" prop="remark">
                <el-input
                  v-model="form.remark"
                  type="textarea"
                  :rows="2"
                  maxlength="100"
                  show-word-limit
                  placeholder="例如：整包书本、易拉罐压块、换季衣物"
                />
              </el-form-item>
            </el-form>

            <div class="submit-row">
              <el-button
                type="primary"
                size="large"
                :loading="submitting"
                :disabled="!selectedCategory || !form.weight"
                @click="submit"
              >
                确认登记并发放积分
              </el-button>
              <span class="muted">积分由服务端按类别系数计算，请求中的 points 字段一律忽略。</span>
            </div>
          </div>
        </section>

        <aside class="panel summary-panel">
          <div class="panel-head">
            <div>
              <h3 class="panel-title">本次预计</h3>
              <p class="panel-sub">提交前核对重量与类别</p>
            </div>
          </div>

          <div class="summary-body">
            <div class="summary-line">
              <span>垃圾类别</span>
              <strong>{{ selectedCategory?.name || '未选择' }}</strong>
            </div>
            <div class="summary-line">
              <span>积分系数</span>
              <strong class="num">{{ selectedCategory ? `${selectedCategory.pointsPerKg} 分 / kg` : '—' }}</strong>
            </div>
            <div class="summary-line">
              <span>录入重量</span>
              <strong class="num">{{ weightText }} kg</strong>
            </div>

            <div class="summary-total">
              <span>预计获得积分</span>
              <strong class="num">{{ estimate }}<small>分</small></strong>
              <p>计算方式：round(重量 × 系数)，最低 1 分</p>
            </div>

            <div class="summary-balance">
              登记后用户余额预计
              <strong class="num">{{ formatNumber(projectedBalance) }}</strong> 分
            </div>

            <el-alert
              type="info"
              :closable="false"
              show-icon
              title="登记后 5 分钟内可自助撤销"
              description="超过 5 分钟如发现错误，需联系管理员授权撤销。"
            />
          </div>
        </aside>
      </div>

      <el-dialog v-model="successVisible" width="410px" :show-close="false" align-center>
        <div class="success">
          <span class="success__check"><el-icon><Check /></el-icon></span>
          <h3>登记成功</h3>
          <p class="success__points num">+{{ result?.points ?? 0 }}<small>积分</small></p>
          <p class="success__meta">
            {{ result?.categoryName }} {{ result?.weight?.toFixed(2) }} kg ·
            用户余额 {{ formatNumber(result?.balanceAfter ?? 0) }} 分
          </p>
          <p class="success__no mono">流水号 {{ result?.recordNo }}</p>
          <div class="success__actions">
            <el-button @click="viewRecords">查看流水</el-button>
            <el-button type="primary" @click="continueNext">继续下一单</el-button>
          </div>
        </div>
      </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Aim, List } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { listCategories } from '@/api/category'
import { createDelivery } from '@/api/delivery'
import { formatNumber } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const categories = ref([])
const formRef = ref(null)
const submitting = ref(false)
const successVisible = ref(false)
const result = ref(null)
const user = ref(readUserFromQuery())

const form = reactive({ categoryId: null, weight: null, remark: '' })

const rules = {
  categoryId: [{ required: true, message: '请选择垃圾类别', trigger: 'change' }],
  weight: [
    {
      validator: (rule, value, callback) => {
        if (value === null || value === undefined || value === '') return callback(new Error('请输入重量'))
        if (value < 0.01 || value > 100) return callback(new Error('重量需在 0.01–100 kg 之间'))
        return callback()
      },
      trigger: 'blur'
    }
  ]
}

const currentUser = computed(() => user.value)
const selectedCategory = computed(
  () => categories.value.find((item) => item.categoryId === form.categoryId) || null
)
const weightText = computed(() => (form.weight ? Number(form.weight).toFixed(2) : '0.00'))
const estimate = computed(() => {
  if (!selectedCategory.value || !form.weight) return 0
  return Math.max(1, Math.round(Number(form.weight) * selectedCategory.value.pointsPerKg))
})
const projectedBalance = computed(() => (user.value?.pointsBalance || 0) + estimate.value)

function readUserFromQuery() {
  if (!route.query.userId) return null
  return {
    userId: Number(route.query.userId),
    nickname: String(route.query.nickname || '用户'),
    userCode: String(route.query.userCode || '—'),
    phoneMasked: String(route.query.phoneMasked || ''),
    pointsBalance: Number(route.query.pointsBalance) || 0
  }
}

watch(
  () => route.query,
  () => {
    user.value = readUserFromQuery()
  },
  { deep: true }
)

onMounted(async () => {
  const list = await listCategories({ enabledOnly: true })
  categories.value = list.filter((item) => item.status === 'ENABLED')
})

async function submit() {
  if (submitting.value || !currentUser.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const data = await createDelivery({
      userId: currentUser.value.userId,
      categoryId: form.categoryId,
      weight: form.weight,
      remark: form.remark.trim() || null
    })
    result.value = data
    user.value.pointsBalance = data.balanceAfter
    successVisible.value = true
  } finally {
    submitting.value = false
  }
}

function continueNext() {
  successVisible.value = false
  form.weight = null
  form.remark = ''
  formRef.value?.clearValidate()
}

function viewRecords() {
  successVisible.value = false
  router.push('/workbench/records')
}
</script>

<style scoped>
.delivery-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.75fr);
  gap: 20px;
  align-items: start;
}

.user-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 22px;
  border-bottom: 1px solid var(--line);
  background: linear-gradient(180deg, #f7fbf9, #fff);
  flex-wrap: wrap;
}

.user-bar__avatar {
  width: 42px;
  height: 42px;
  font-size: 16px;
}

.user-bar__meta {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}

.user-bar__meta strong {
  font-size: 14.5px;
  font-weight: 650;
}

.user-bar__meta small {
  color: var(--ink-500);
  font-size: 11px;
}

.user-bar__balance {
  margin-left: auto;
  color: var(--ink-500);
  font-size: 12px;
}

.user-bar__balance strong {
  color: var(--brand-700);
  font-size: 16px;
  font-weight: 650;
}

.form-body {
  padding: 22px 22px 24px;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
  width: 100%;
}

.category-card {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 14px 16px;
  border: 1.5px solid var(--line);
  border-radius: 12px;
  background: #fff;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.16s ease, background 0.16s ease, box-shadow 0.16s ease;
}

.category-card:hover {
  border-color: #9fcdb4;
  background: #fbfdfb;
}

.category-card.active {
  border-color: var(--brand-600);
  background: var(--brand-50);
  box-shadow: 0 0 0 3px rgba(22, 121, 76, 0.08);
}

.category-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.category-card__name {
  color: var(--ink-900);
  font-size: 14px;
  font-weight: 600;
}

.category-card__check {
  color: var(--brand-600);
  font-size: 14px;
}

.category-card__code {
  color: var(--ink-400);
  font-size: 10px;
  letter-spacing: 0.06em;
}

.category-card__rate {
  margin-top: 4px;
  color: var(--brand-700);
  font-size: 17px;
  font-weight: 650;
}

.category-card__rate small {
  margin-left: 2px;
  color: var(--ink-400);
  font-size: 10px;
  font-weight: 500;
}

.weight-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.weight-row :deep(.el-input-number) {
  width: 190px;
}

.weight-row :deep(.el-input__inner) {
  font-size: 17px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.weight-hint {
  color: var(--ink-400);
  font-size: 11.5px;
}

.submit-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  padding-top: 6px;
  border-top: 1px solid var(--line);
  margin-top: 4px;
}

.submit-row .el-button {
  height: 46px;
  min-width: 190px;
  margin-top: 16px;
}

.submit-row .muted {
  margin-top: 16px;
  font-size: 11.5px;
}

.summary-panel {
  position: sticky;
  top: 12px;
}

.summary-body {
  padding: 20px 22px 24px;
}

.summary-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 12.5px;
}

.summary-line span {
  color: var(--ink-500);
}

.summary-line strong {
  font-weight: 600;
}

.summary-total {
  margin: 18px 0 12px;
  padding: 18px;
  border-radius: 13px;
  background:
    radial-gradient(280px 160px at 100% 0%, rgba(63, 207, 142, 0.16), transparent 70%),
    linear-gradient(150deg, #12603d, #0d3a26);
  color: #fff;
}

.summary-total > span {
  color: rgba(226, 240, 232, 0.66);
  font-size: 11.5px;
}

.summary-total > strong {
  display: block;
  margin-top: 4px;
  font-size: 40px;
  font-weight: 680;
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.summary-total > strong small {
  margin-left: 5px;
  color: rgba(226, 240, 232, 0.6);
  font-size: 12px;
  font-weight: 500;
}

.summary-total p {
  margin-top: 8px;
  color: rgba(226, 240, 232, 0.42);
  font-size: 10.5px;
}

.summary-balance {
  padding: 10px 12px;
  border-radius: 9px;
  background: #f8faf9;
  color: var(--ink-500);
  font-size: 12px;
  margin-bottom: 16px;
}

.summary-balance strong {
  color: var(--brand-700);
  font-size: 14px;
}

.empty-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64px 30px;
  text-align: center;
}

.empty-panel__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 66px;
  height: 66px;
  border-radius: 50%;
  background: var(--brand-50);
  border: 1px dashed #b9dcc9;
  color: var(--brand-600);
  font-size: 26px;
  margin-bottom: 18px;
}

.empty-panel h2 {
  font-size: 18px;
  font-weight: 650;
}

.empty-panel p {
  margin: 8px 0 22px;
  color: var(--ink-500);
  font-size: 13px;
}

.success {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 8px 8px;
  text-align: center;
}

.success__check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: var(--brand-100);
  color: var(--brand-600);
  font-size: 28px;
  margin-bottom: 14px;
}

.success h3 {
  font-size: 18px;
  font-weight: 650;
}

.success__points {
  margin-top: 10px;
  color: var(--brand-700);
  font-size: 40px;
  font-weight: 680;
  letter-spacing: -0.02em;
  line-height: 1.1;
}

.success__points small {
  margin-left: 4px;
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-400);
}

.success__meta {
  margin-top: 10px;
  color: var(--ink-500);
  font-size: 12.5px;
}

.success__no {
  margin-top: 4px;
  color: var(--ink-400);
  font-size: 11px;
}

.success__actions {
  display: flex;
  gap: 10px;
  margin-top: 24px;
  width: 100%;
}

.success__actions .el-button {
  flex: 1;
  height: 42px;
}

@media (max-width: 1120px) {
  .delivery-grid {
    grid-template-columns: 1fr;
  }

  .summary-panel {
    position: static;
  }
}
</style>
