<template>
  <div class="page">
    <PageHeader title="商品管理" description="发布商品、维护图片与库存、控制上下架。有订单的商品只能下架，不能删除。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增商品</el-button>
      </template>
    </PageHeader>

    <section class="panel">
      <div class="toolbar">
        <div class="toolbar__filters">
          <el-input
            v-model="query.keyword"
            placeholder="搜索商品名称"
            clearable
            :prefix-icon="Search"
            style="width: 220px"
            @keyup.enter="search"
          />
          <el-select v-model="query.sort" style="width: 150px" @change="search">
            <el-option label="最新创建" value="created_desc" />
            <el-option label="积分从低到高" value="points_asc" />
            <el-option label="积分从高到低" value="points_desc" />
          </el-select>
          <el-checkbox v-model="query.onlyAvailable" @change="search">只看可兑换</el-checkbox>
          <el-button type="primary" plain @click="search">查询</el-button>
          <el-button text @click="resetQuery">重置</el-button>
        </div>
        <span class="muted">共 {{ total }} 个商品</span>
      </div>

      <div class="table-wrap">
        <el-table :data="products" v-loading="loading" empty-text="暂无商品，点击右上角新增">
          <el-table-column label="商品" min-width="260">
            <template #default="{ row }">
              <div class="product-cell">
                <img v-if="row.coverUrl" class="thumb" :src="row.coverUrl" :alt="row.name" />
                <span v-else class="thumb thumb--empty"><el-icon><Picture /></el-icon></span>
                <div>
                  <div class="cell-title">{{ row.name }}</div>
                  <div class="cell-sub">{{ row.spec || '暂未填写规格' }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="所需积分" width="120" align="right">
            <template #default="{ row }">
              <span class="price num">{{ formatNumber(row.pointsPrice) }}</span>
              <span class="faint"> 分</span>
            </template>
          </el-table-column>
          <el-table-column label="库存" width="110" align="right">
            <template #default="{ row }">
              <span
                class="num stock"
                :class="{ 'is-zero': row.stock === 0, 'is-low': row.stock > 0 && row.stock <= 5 }"
              >
                {{ row.stock }}
              </span>
              <div v-if="row.stock === 0" class="cell-sub text-danger">已兑完</div>
              <div v-else-if="row.stock <= 5" class="cell-sub text-warn">库存紧张</div>
            </template>
          </el-table-column>
          <el-table-column label="上下架" width="120" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 'ON'"
                width="58"
                inline-prompt
                active-text="上架"
                inactive-text="下架"
                class="status-switch"
                :before-change="() => toggleStatus(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="openStock(row)">库存</el-button>
              <el-button link type="danger" @click="remove(row)">删除</el-button>
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
    </section>

    <!-- 新增 / 编辑 -->
    <el-drawer v-model="drawerVisible" :title="editing ? '编辑商品' : '新增商品'" size="520px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item label="商品封面">
          <UploadImage v-model="form.coverUrl" scene="PRODUCT" />
        </el-form-item>
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit placeholder="如：环保帆布袋" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="所需积分" prop="pointsPrice">
            <el-input-number v-model="form.pointsPrice" :min="1" :max="100000" controls-position="right" style="width: 180px" />
          </el-form-item>
          <el-form-item v-if="!editing" label="初始库存" prop="stock">
            <el-input-number v-model="form.stock" :min="0" :max="99999" controls-position="right" style="width: 180px" />
          </el-form-item>
        </div>
        <el-form-item label="规格说明">
          <el-input v-model="form.spec" maxlength="100" placeholder="如：38 × 42 cm / 棉麻" />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="介绍商品材质、用途等" />
        </el-form-item>
        <el-form-item label="兑换须知">
          <el-input v-model="form.exchangeNotice" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="如：兑换后 30 天内到站点领取" />
        </el-form-item>
        <el-alert
          v-if="editing"
          type="info"
          :closable="false"
          show-icon
          title="库存不在编辑中修改"
          description="库存请通过「库存」操作单独调整，避免与兑换并发扣减冲突。"
        />
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ editing ? '保存修改' : '创建商品' }}</el-button>
      </template>
    </el-drawer>

    <!-- 库存调整 -->
    <el-dialog v-model="stockVisible" title="调整库存" width="480px" align-center>
      <div class="stock-summary">
        <img v-if="stockTarget?.coverUrl" class="thumb" :src="stockTarget.coverUrl" alt="" />
        <div>
          <strong>{{ stockTarget?.name }}</strong>
          <p class="muted">当前库存 <b class="num">{{ stockTarget?.stock }}</b> 件</p>
        </div>
      </div>

      <el-form label-position="top">
        <el-form-item label="调整模式">
          <el-radio-group v-model="stockForm.mode">
            <el-radio-button value="INCR">增减</el-radio-button>
            <el-radio-button value="SET">设为指定值</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="stockForm.mode === 'SET' ? '目标库存' : '变化量（可为负）'">
          <el-input-number
            v-model="stockForm.value"
            :min="stockForm.mode === 'SET' ? 0 : -99999"
            :max="99999"
            controls-position="right"
            style="width: 180px"
          />
          <span class="stock-preview">
            调整后库存
            <b :class="{ 'text-danger': previewStock < 0 }" class="num">{{ previewStock }}</b> 件
          </span>
        </el-form-item>
        <el-form-item label="调整原因" required>
          <el-input
            v-model="stockForm.reason"
            maxlength="100"
            show-word-limit
            placeholder="如：新一批入库 / 月末盘点 / 破损损耗"
          />
        </el-form-item>
      </el-form>

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="库存不足兑换时会返回 3002"
        description="调整库存不会影响已生成的待核销订单。"
      />

      <template #footer>
        <el-button @click="stockVisible = false">取消</el-button>
        <el-button type="primary" :loading="stockSaving" @click="saveStock">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import UploadImage from '@/components/UploadImage.vue'
import {
  adjustProductStock,
  createProduct,
  deleteProduct,
  listProducts,
  updateProduct,
  updateProductStatus
} from '@/api/product'
import { formatDateTime, formatNumber } from '@/utils/format'

const products = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', sort: 'created_desc', onlyAvailable: false, page: 1, size: 10 })

const drawerVisible = ref(false)
const editing = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref(null)

const stockVisible = ref(false)
const stockSaving = ref(false)
const stockTarget = ref(null)
const stockForm = reactive({ mode: 'INCR', value: 0, reason: '' })

const form = reactive({
  name: '',
  coverUrl: '',
  pointsPrice: 100,
  stock: 0,
  spec: '',
  description: '',
  exchangeNotice: ''
})

const formRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  pointsPrice: [{ required: true, message: '请输入所需积分', trigger: 'change' }],
  stock: [{ required: true, message: '请输入初始库存', trigger: 'change' }]
}

const previewStock = computed(() => {
  if (!stockTarget.value) return 0
  return stockForm.mode === 'SET'
    ? Number(stockForm.value) || 0
    : stockTarget.value.stock + (Number(stockForm.value) || 0)
})

async function load() {
  loading.value = true
  try {
    const data = await listProducts({
      page: query.page,
      size: query.size,
      keyword: query.keyword.trim() || undefined,
      sort: query.sort,
      onlyAvailable: query.onlyAvailable
    })
    products.value = data.list
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
  query.sort = 'created_desc'
  query.onlyAvailable = false
  search()
}

function openCreate() {
  editing.value = false
  editingId.value = null
  Object.assign(form, {
    name: '',
    coverUrl: '',
    pointsPrice: 100,
    stock: 10,
    spec: '',
    description: '',
    exchangeNotice: ''
  })
  formRef.value?.clearValidate()
  drawerVisible.value = true
}

function openEdit(row) {
  editing.value = true
  editingId.value = row.productId
  Object.assign(form, {
    name: row.name,
    coverUrl: row.coverUrl || '',
    pointsPrice: row.pointsPrice,
    stock: row.stock,
    spec: row.spec || '',
    description: row.description || '',
    exchangeNotice: row.exchangeNotice || ''
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
      await updateProduct(editingId.value, {
        name: form.name,
        coverUrl: form.coverUrl || null,
        pointsPrice: form.pointsPrice,
        spec: form.spec || null,
        description: form.description || null,
        exchangeNotice: form.exchangeNotice || null
      })
      ElMessage.success('商品信息已更新')
    } else {
      await createProduct({
        name: form.name,
        coverUrl: form.coverUrl || null,
        pointsPrice: form.pointsPrice,
        stock: form.stock,
        spec: form.spec || null,
        description: form.description || null,
        exchangeNotice: form.exchangeNotice || null
      })
      ElMessage.success('商品已创建')
    }
    drawerVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 'ON' ? 'OFF' : 'ON'
  const label = next === 'OFF' ? '下架' : '上架'
  try {
    await ElMessageBox.confirm(
      next === 'OFF'
        ? `确认下架「${row.name}」？下架后用户端不可兑换，但已生成的待核销订单仍可核销。`
        : `确认上架「${row.name}」？上架后用户可在积分商城兑换。`,
      `${label}商品`,
      { confirmButtonText: label, cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return false
  }
  try {
    await updateProductStatus(row.productId, { status: next })
    row.status = next
    ElMessage.success(`已${label}「${row.name}」`)
    return true
  } catch {
    return false
  }
}

function openStock(row) {
  stockTarget.value = row
  stockForm.mode = 'INCR'
  stockForm.value = 0
  stockForm.reason = ''
  stockVisible.value = true
}

async function saveStock() {
  if (!stockForm.reason.trim()) {
    ElMessage.warning('请填写库存调整原因')
    return
  }
  if (!Number.isInteger(Number(stockForm.value))) {
    ElMessage.warning('调整数量必须为整数')
    return
  }
  if (previewStock.value < 0) {
    ElMessage.warning('调整后库存不能为负')
    return
  }
  stockSaving.value = true
  try {
    const data = await adjustProductStock(stockTarget.value.productId, {
      mode: stockForm.mode,
      value: Number(stockForm.value),
      reason: stockForm.reason.trim()
    })
    ElMessage.success(`库存已更新：${data.stockBefore} → ${data.stock}`)
    stockVisible.value = false
    await load()
  } finally {
    stockSaving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除商品「${row.name}」？仅没有关联订单的商品可删除，否则请改为下架。`,
      '删除商品',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await deleteProduct(row.productId)
  ElMessage.success('商品已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thumb--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-400);
  font-size: 16px;
}

.price {
  color: var(--brand-700);
  font-size: 15px;
  font-weight: 650;
}

.status-switch {
  --el-switch-on-color: #16794c;
  --el-switch-off-color: #b6c2ba;
}

.status-switch :deep(.el-switch__core) {
  min-width: 58px;
}

.status-switch :deep(.el-switch__inner) {
  font-size: 10.5px;
  letter-spacing: 0;
}

.stock {
  font-size: 14px;
  font-weight: 600;
}

.stock.is-low {
  color: var(--amber-700);
}

.stock.is-zero {
  color: var(--red-600);
}

.form-row {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.stock-summary {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  margin-bottom: 18px;
  border: 1px solid var(--line);
  border-radius: 11px;
  background: #fbfdfb;
}

.stock-summary strong {
  font-size: 14px;
  font-weight: 600;
}

.stock-summary p {
  margin-top: 2px;
  font-size: 12px;
}

.stock-preview {
  margin-left: 14px;
  color: var(--ink-500);
  font-size: 12px;
}

.stock-preview b {
  margin-left: 4px;
  color: var(--brand-700);
  font-size: 15px;
}
</style>
