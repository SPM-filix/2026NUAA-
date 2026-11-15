<template>
  <div class="page">
    <PageHeader
      title="积分规则"
      description="每个类别保留完整规则版本。调价仅影响此后新流水，历史流水积分不重算。"
    >
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="reloadAll">刷新</el-button>
      </template>
    </PageHeader>

    <div class="rules-grid">
      <aside class="panel category-panel">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">类别</h3>
            <p class="panel-sub">选择类别查看规则</p>
          </div>
        </div>
        <div class="category-list">
          <button
            v-for="category in categories"
            :key="category.categoryId"
            type="button"
            class="category-item"
            :class="{ active: category.categoryId === selectedId }"
            @click="selectCategory(category.categoryId)"
          >
            <span class="category-item__avatar">{{ category.name.slice(0, 1) }}</span>
            <span class="category-item__info">
              <strong>{{ category.name }}</strong>
              <small class="mono">{{ category.code }}</small>
            </span>
            <span class="category-item__rate num">
              {{ category.pointsPerKg }}
              <small>分/kg</small>
            </span>
          </button>
          <el-empty v-if="!loading && !categories.length" description="暂无类别" :image-size="60" />
        </div>
      </aside>

      <div class="rules-main">
        <section v-if="selected" class="current-card">
          <div class="current-card__top">
            <span class="current-card__eyebrow">当前生效规则</span>
            <StatusTag :value="selected.status" kind="enable" />
          </div>
          <div class="current-card__body">
            <div>
              <h2>{{ selected.name }}</h2>
              <p>生效于 {{ formatDateTime(selected.ruleEffectiveFrom) }}</p>
            </div>
            <div class="current-card__rate">
              <strong class="num">{{ selected.pointsPerKg }}</strong>
              <span>分 / {{ selected.unit || 'kg' }}</span>
            </div>
          </div>
          <div class="current-card__foot">
            <span>新规则只作用于此后登记的流水，已发放积分不会被追溯修改。</span>
            <el-button type="primary" @click="openRuleDialog">调整积分系数</el-button>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div>
              <h3 class="panel-title">规则历史</h3>
              <p class="panel-sub">按生效时间倒序，保留配置人与创建时间</p>
            </div>
          </div>
          <div class="table-wrap">
            <el-table :data="rules" v-loading="rulesLoading" empty-text="该类别暂无规则记录">
              <el-table-column label="积分系数" width="150">
                <template #default="{ row }">
                  <span class="history-rate num">{{ row.pointsPerKg }}</span>
                  <span class="muted"> 分 / kg</span>
                </template>
              </el-table-column>
              <el-table-column label="生效时间" min-width="180">
                <template #default="{ row }">{{ formatDateTime(row.effectiveFrom) }}</template>
              </el-table-column>
              <el-table-column prop="createdBy" label="配置人" width="140" />
              <el-table-column label="创建时间" min-width="180">
                <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.ruleId === currentRuleId" type="success" size="small" round>当前生效</el-tag>
                  <span v-else class="faint">历史版本</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </section>
      </div>
    </div>

    <el-dialog v-model="ruleVisible" title="调整积分系数" width="480px" align-center>
      <div class="rule-dialog-head">
        <span>类别</span>
        <strong>{{ selected?.name }}</strong>
        <span class="rule-dialog-head__rate num">{{ selected?.pointsPerKg }} 分/kg → {{ form.pointsPerKg }} 分/kg</span>
      </div>

      <el-form ref="ruleFormRef" :model="form" :rules="ruleRules" label-position="top">
        <el-form-item label="新的每公斤积分" prop="pointsPerKg">
          <el-input-number
            v-model="form.pointsPerKg"
            :min="1"
            :max="1000"
            controls-position="right"
            style="width: 180px"
          />
          <span class="field-tip inline">范围 1–1000 分</span>
        </el-form-item>
        <el-form-item label="生效时间" prop="effectiveFrom">
          <el-date-picker
            v-model="form.effectiveFrom"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="默认立即生效"
            style="width: 240px"
          />
        </el-form-item>
      </el-form>

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="该调整仅影响此后登记的新流水"
        description="历史投递记录已按当时系数发放积分，不会重新计算；生效时间不得早于当前规则。"
      />

      <template #footer>
        <el-button @click="ruleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRule">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { listCategories, listCategoryRules, setCategoryRule } from '@/api/category'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const categories = ref([])
const selectedId = ref(Number(route.query.categoryId) || null)
const rules = ref([])
const loading = ref(false)
const rulesLoading = ref(false)
const ruleVisible = ref(false)
const saving = ref(false)
const ruleFormRef = ref(null)

const form = reactive({ pointsPerKg: 10, effectiveFrom: '' })
const ruleRules = {
  pointsPerKg: [{ required: true, message: '请输入积分系数', trigger: 'change' }],
  effectiveFrom: [{ required: true, message: '请选择生效时间', trigger: 'change' }]
}

const selected = computed(() => categories.value.find((item) => item.categoryId === selectedId.value))
const currentRuleId = computed(() => {
  if (!selected.value) return null
  const match = rules.value.find(
    (item) => item.pointsPerKg === selected.value.pointsPerKg &&
      item.effectiveFrom === selected.value.ruleEffectiveFrom
  )
  return match?.ruleId ?? rules.value[0]?.ruleId ?? null
})

function pad(value) {
  return String(value).padStart(2, '0')
}

function nowInput() {
  const date = new Date()
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}:00`
}

async function loadCategories() {
  loading.value = true
  try {
    categories.value = await listCategories()
    if (!selectedId.value || !categories.value.some((item) => item.categoryId === selectedId.value)) {
      selectedId.value = categories.value[0]?.categoryId ?? null
    }
  } finally {
    loading.value = false
  }
}

async function loadRules() {
  if (!selectedId.value) {
    rules.value = []
    return
  }
  rulesLoading.value = true
  try {
    const data = await listCategoryRules(selectedId.value, { page: 1, size: 20 })
    rules.value = data.list
  } finally {
    rulesLoading.value = false
  }
}

async function reloadAll() {
  await loadCategories()
  await loadRules()
}

function selectCategory(id) {
  selectedId.value = id
  router.replace({ path: '/config/rules', query: { categoryId: id } })
}

function openRuleDialog() {
  form.pointsPerKg = selected.value?.pointsPerKg ?? 10
  form.effectiveFrom = nowInput()
  ruleVisible.value = true
  ruleFormRef.value?.clearValidate()
}

async function saveRule() {
  try {
    await ruleFormRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    await setCategoryRule(selectedId.value, {
      pointsPerKg: form.pointsPerKg,
      effectiveFrom: `${form.effectiveFrom.replace(' ', 'T')}+08:00`
    })
    ElMessage.success('积分系数已更新，仅影响此后新流水')
    ruleVisible.value = false
    await reloadAll()
  } finally {
    saving.value = false
  }
}

watch(selectedId, loadRules)

onMounted(async () => {
  await loadCategories()
  await loadRules()
})
</script>

<style scoped>
.rules-grid {
  display: grid;
  grid-template-columns: minmax(260px, 0.62fr) minmax(0, 1.7fr);
  gap: 20px;
  align-items: start;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  padding: 11px 12px;
  border: 1px solid transparent;
  border-radius: 11px;
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: all 0.16s ease;
}

.category-item:hover {
  background: #f8faf9;
}

.category-item.active {
  background: var(--brand-50);
  border-color: #cfe8db;
}

.category-item__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--brand-100);
  color: var(--brand-700);
  font-size: 13px;
  font-weight: 650;
  flex: none;
}

.category-item__info {
  flex: 1;
  min-width: 0;
}

.category-item__info strong {
  display: block;
  color: var(--ink-900);
  font-size: 13px;
  font-weight: 600;
}

.category-item__info small {
  display: block;
  color: var(--ink-400);
  font-size: 10px;
}

.category-item__rate {
  color: var(--ink-700);
  font-size: 16px;
  font-weight: 650;
}

.category-item__rate small {
  color: var(--ink-400);
  font-size: 9.5px;
  font-weight: 500;
}

.rules-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.current-card {
  padding: 24px 26px 20px;
  border-radius: var(--radius-lg);
  background:
    radial-gradient(500px 260px at 100% 0%, rgba(63, 207, 142, 0.2), transparent 65%),
    linear-gradient(150deg, #12603d, #0d3a26);
  color: #fff;
  box-shadow: 0 18px 40px -32px rgba(11, 42, 28, 0.8);
}

.current-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.current-card__eyebrow {
  color: #7cebb5;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.14em;
}

.current-card__body {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.current-card__body h2 {
  font-size: 21px;
  font-weight: 650;
}

.current-card__body p {
  margin-top: 4px;
  color: rgba(226, 240, 232, 0.55);
  font-size: 11.5px;
}

.current-card__rate {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.current-card__rate strong {
  font-size: 46px;
  font-weight: 680;
  line-height: 1;
  letter-spacing: -0.02em;
}

.current-card__rate span {
  color: rgba(226, 240, 232, 0.56);
  font-size: 11.5px;
}

.current-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  flex-wrap: wrap;
}

.current-card__foot > span {
  color: rgba(226, 240, 232, 0.48);
  font-size: 11px;
}

.history-rate {
  color: var(--brand-700);
  font-size: 15px;
  font-weight: 650;
}

.rule-dialog-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f8faf9;
  border: 1px solid var(--line);
  color: var(--ink-500);
  font-size: 12.5px;
}

.rule-dialog-head strong {
  color: var(--ink-900);
  font-weight: 600;
}

.rule-dialog-head__rate {
  margin-left: auto;
  color: var(--brand-700);
  font-weight: 600;
}

.field-tip {
  color: var(--ink-400);
  font-size: 11px;
}

.field-tip.inline {
  display: inline-block;
  margin-left: 10px;
}

.el-alert {
  margin-top: 4px;
}

@media (max-width: 1100px) {
  .rules-grid {
    grid-template-columns: 1fr;
  }
}
</style>
