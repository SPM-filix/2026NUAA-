<template>
  <div class="page">
    <PageHeader title="垃圾类别" description="维护回收类别与积分系数。类别被流水引用后只能停用，不能删除。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增类别</el-button>
      </template>
    </PageHeader>

    <section class="panel">
      <div class="toolbar">
        <div class="toolbar__filters">
          <el-input
            v-model="keyword"
            placeholder="搜索名称或编码"
            clearable
            :prefix-icon="Search"
            style="width: 220px"
          />
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </div>
        <span class="muted">共 {{ categories.length }} 个类别</span>
      </div>

      <div class="table-wrap">
        <el-table :data="filteredCategories" v-loading="loading" empty-text="暂无类别，点击右上角新增">
          <el-table-column label="排序" width="70" align="center">
            <template #default="{ row }"><span class="num faint">{{ row.sort }}</span></template>
          </el-table-column>
          <el-table-column label="类别" min-width="160">
            <template #default="{ row }">
              <div class="cell-title">{{ row.name }}</div>
              <div class="cell-sub mono">{{ row.code }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="90">
            <template #default="{ row }">{{ row.unit || 'kg' }}</template>
          </el-table-column>
          <el-table-column label="当前积分系数" min-width="180">
            <template #default="{ row }">
              <span class="rate num">{{ row.pointsPerKg }}</span>
              <span class="rate-unit">分 / {{ row.unit || 'kg' }}</span>
              <div class="cell-sub">生效于 {{ formatDateTime(row.ruleEffectiveFrom) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }"><StatusTag :value="row.status" kind="enable" /></template>
          </el-table-column>
          <el-table-column label="操作" width="240" align="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="goRules(row)">调价</el-button>
              <el-button link :type="row.status === 'ENABLED' ? 'warning' : 'success'" @click="toggleStatus(row)">
                {{ row.status === 'ENABLED' ? '停用' : '启用' }}
              </el-button>
              <el-button link type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel-note">
        <el-icon><InfoFilled /></el-icon>
        停用后回收员端不再可选该类别，历史流水与积分不变；删除仅对未被流水引用的类别生效。
      </div>
    </section>

    <el-drawer v-model="drawerVisible" :title="editing ? '编辑类别' : '新增类别'" size="440px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item label="类别名称" prop="name">
          <el-input v-model="form.name" maxlength="20" show-word-limit placeholder="如：废纸" />
        </el-form-item>
        <el-form-item label="类别编码" prop="code">
          <el-input
            v-model="form.code"
            :disabled="editing"
            maxlength="20"
            placeholder="大写字母与下划线，如 PAPER"
            @input="form.code = form.code.toUpperCase().replace(/[^A-Z_]/g, '')"
          />
          <p class="field-tip">{{ editing ? '编码创建后不可修改，避免历史数据口径漂移。' : '创建后不可修改。' }}</p>
        </el-form-item>
        <el-form-item label="计量单位" prop="unit">
          <el-input v-model="form.unit" maxlength="10" placeholder="默认 kg" />
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" style="width: 160px" />
          <span class="field-tip inline">越小越靠前</span>
        </el-form-item>
        <el-form-item v-if="!editing" label="初始积分系数" prop="pointsPerKg">
          <el-input-number v-model="form.pointsPerKg" :min="1" :max="1000" controls-position="right" style="width: 160px" />
          <span class="field-tip inline">分 / kg</span>
        </el-form-item>
        <el-form-item label="图标 URL（可选）">
          <el-input v-model="form.iconUrl" maxlength="255" placeholder="上传后的图片地址" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ editing ? '保存修改' : '创建类别' }}</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  createCategory,
  deleteCategory,
  listCategories,
  updateCategory,
  updateCategoryStatus
} from '@/api/category'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

const categories = ref([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const drawerVisible = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref(null)
const editingId = ref(null)

const form = reactive({
  name: '',
  code: '',
  unit: 'kg',
  sort: 0,
  pointsPerKg: 10,
  iconUrl: ''
})

const formRules = {
  name: [{ required: true, message: '请输入类别名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入类别编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z_]*$/, message: '编码需为大写字母与下划线', trigger: 'blur' }
  ],
  pointsPerKg: [{ required: true, message: '请设置初始积分系数', trigger: 'change' }]
}

const filteredCategories = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return categories.value
    .filter((item) => !statusFilter.value || item.status === statusFilter.value)
    .filter(
      (item) =>
        !text ||
        item.name.toLowerCase().includes(text) ||
        item.code.toLowerCase().includes(text)
    )
    .sort((a, b) => a.sort - b.sort)
})

async function load() {
  loading.value = true
  try {
    categories.value = await listCategories()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    name: '',
    code: '',
    unit: 'kg',
    sort: 0,
    pointsPerKg: 10,
    iconUrl: ''
  })
  formRef.value?.clearValidate()
}

function openCreate() {
  editing.value = false
  editingId.value = null
  resetForm()
  drawerVisible.value = true
}

function openEdit(row) {
  editing.value = true
  editingId.value = row.categoryId
  Object.assign(form, {
    name: row.name,
    code: row.code,
    unit: row.unit || 'kg',
    sort: row.sort ?? 0,
    pointsPerKg: row.pointsPerKg,
    iconUrl: row.iconUrl || ''
  })
  formRef.value?.clearValidate()
  drawerVisible.value = true
}

async function save() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateCategory(editingId.value, {
        name: form.name,
        unit: form.unit,
        sort: form.sort,
        iconUrl: form.iconUrl || null
      })
      ElMessage.success('类别信息已更新')
    } else {
      await createCategory({
        name: form.name,
        code: form.code,
        unit: form.unit,
        sort: form.sort,
        pointsPerKg: form.pointsPerKg,
        iconUrl: form.iconUrl || null
      })
      ElMessage.success('类别已创建')
    }
    drawerVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const action = nextStatus === 'DISABLED' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(
      nextStatus === 'DISABLED'
        ? `停用「${row.name}」后，回收员端将不再出现该类别，历史流水和积分不受影响。`
        : `启用「${row.name}」后，回收员可重新选择该类别进行投递登记。`,
      `${action}类别`,
      { confirmButtonText: action, cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await updateCategoryStatus(row.categoryId, { status: nextStatus })
  ElMessage.success(`已${action}「${row.name}」`)
  await load()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除类别「${row.name}」？仅未被流水引用的类别可以删除，否则请改为停用。`,
      '删除类别',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await deleteCategory(row.categoryId)
  ElMessage.success('类别已删除')
  await load()
}

function goRules(row) {
  router.push({ path: '/config/rules', query: { categoryId: row.categoryId } })
}

onMounted(load)
</script>

<style scoped>
.rate {
  color: var(--brand-700);
  font-size: 17px;
  font-weight: 650;
}

.rate-unit {
  margin-left: 3px;
  color: var(--ink-400);
  font-size: 10.5px;
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

.field-tip {
  margin-top: 4px;
  color: var(--ink-400);
  font-size: 11px;
  line-height: 1.6;
}

.field-tip.inline {
  display: inline-block;
  margin: 0 0 0 10px;
}
</style>
