<template>
  <div class="page">
    <PageHeader title="回收站点" description="维护回收站点信息与开放状态。停用站点不影响已有回收员的归属记录。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增站点</el-button>
      </template>
    </PageHeader>

    <section class="panel">
      <div class="toolbar">
        <div class="toolbar__filters">
          <el-input
            v-model="keyword"
            placeholder="搜索站点名称或地址"
            clearable
            :prefix-icon="Search"
            style="width: 240px"
          />
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </div>
        <span class="muted">共 {{ sites.length }} 个站点</span>
      </div>

      <div class="table-wrap">
        <el-table :data="filteredSites" v-loading="loading" empty-text="暂无站点，点击右上角新增">
          <el-table-column label="站点" min-width="230">
            <template #default="{ row }">
              <div class="site-cell">
                <span class="site-avatar">{{ row.name.slice(0, 1) }}</span>
                <div>
                  <div class="cell-title">{{ row.name }}</div>
                  <div class="cell-sub">{{ row.address }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="开放时间" width="140">
            <template #default="{ row }">{{ row.openTime || '—' }}</template>
          </el-table-column>
          <el-table-column prop="manager" label="负责人" width="110">
            <template #default="{ row }">{{ row.manager || '—' }}</template>
          </el-table-column>
          <el-table-column label="联系电话" width="150">
            <template #default="{ row }">
              <span class="mono">{{ row.phone || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }"><StatusTag :value="row.status" kind="enable" /></template>
          </el-table-column>
          <el-table-column label="操作" width="170" align="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link :type="row.status === 'ENABLED' ? 'warning' : 'success'" @click="toggleStatus(row)">
                {{ row.status === 'ENABLED' ? '停用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel-note">
        <el-icon><InfoFilled /></el-icon>
        回收员审核通过后必须分配到一个启用中的站点；站点停用后，该站点回收员不能继续登记投递。
      </div>
    </section>

    <el-drawer v-model="drawerVisible" :title="editing ? '编辑站点' : '新增站点'" size="440px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="站点名称" prop="name">
          <el-input v-model="form.name" maxlength="30" show-word-limit placeholder="如：东山回收站" />
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="form.address" maxlength="100" show-word-limit placeholder="如：南京市江宁区将军大道 29 号" />
        </el-form-item>
        <el-form-item label="开放时间">
          <el-input v-model="form.openTime" maxlength="50" placeholder="如：08:00-20:00" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.manager" maxlength="20" placeholder="负责人姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" maxlength="20" placeholder="座机或手机号" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ editing ? '保存修改' : '创建站点' }}</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { createSite, listSites, updateSite, updateSiteStatus } from '@/api/site'

const sites = ref([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const drawerVisible = ref(false)
const editing = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref(null)

const form = reactive({ name: '', address: '', openTime: '', manager: '', phone: '' })

const rules = {
  name: [{ required: true, message: '请输入站点名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入站点地址', trigger: 'blur' }]
}

const filteredSites = computed(() => {
  const text = keyword.value.trim()
  return sites.value
    .filter((item) => !statusFilter.value || item.status === statusFilter.value)
    .filter(
      (item) =>
        !text || item.name.includes(text) || item.address.includes(text) || (item.manager || '').includes(text)
    )
})

async function load() {
  loading.value = true
  try {
    sites.value = await listSites()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, { name: '', address: '', openTime: '', manager: '', phone: '' })
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
  editingId.value = row.siteId
  Object.assign(form, {
    name: row.name,
    address: row.address,
    openTime: row.openTime || '',
    manager: row.manager || '',
    phone: row.phone || ''
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
      await updateSite(editingId.value, { ...form })
      ElMessage.success('站点信息已更新')
    } else {
      await createSite({ ...form })
      ElMessage.success('站点已创建')
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
        ? `停用「${row.name}」后，该站点的回收员将无法继续登记投递，历史数据不受影响。`
        : `启用「${row.name}」后，可重新为回收员分配该站点。`,
      `${action}站点`,
      { confirmButtonText: action, cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await updateSiteStatus(row.siteId, { status: nextStatus })
  ElMessage.success(`已${action}「${row.name}」`)
  await load()
}

onMounted(load)
</script>

<style scoped>
.site-cell {
  display: flex;
  align-items: center;
  gap: 11px;
}

.site-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--brand-100);
  color: var(--brand-700);
  font-size: 13px;
  font-weight: 650;
  flex: none;
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
</style>
