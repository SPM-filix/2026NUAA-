<template>
  <div class="page">
    <PageHeader
      title="公告管理"
      description="发布系统公告与环保科普。正文为纯文本并保留换行，避免富文本带来的 XSS 风险。"
    >
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增公告</el-button>
      </template>
    </PageHeader>

    <section class="panel">
      <div class="toolbar">
        <div class="toolbar__filters">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索标题"
            clearable
            :prefix-icon="Search"
            style="width: 220px"
            @keyup.enter="applyFilters"
          />
          <el-select v-model="filters.type" placeholder="全部类型" clearable style="width: 140px" @change="applyFilters">
            <el-option label="系统公告" value="NOTICE" />
            <el-option label="环保科普" value="KNOWLEDGE" />
          </el-select>
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px" @change="applyFilters">
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已下线" value="OFFLINE" />
          </el-select>
          <el-button type="primary" plain @click="applyFilters">查询</el-button>
          <el-button text @click="resetFilters">重置</el-button>
        </div>
        <span class="muted">共 {{ total }} 条公告</span>
      </div>

      <div class="table-wrap">
        <el-table :data="announcements" v-loading="loading" empty-text="暂无公告，点击右上角新增">
          <el-table-column label="标题" min-width="300">
            <template #default="{ row }">
              <div class="title-cell">
                <img v-if="row.coverUrl" class="thumb" :src="row.coverUrl" alt="" />
                <div>
                  <div class="cell-title">{{ row.title }}</div>
                  <div class="cell-sub">ID {{ row.announcementId }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="120" align="center">
            <template #default="{ row }"><StatusTag :value="row.type" kind="announcementType" /></template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }"><StatusTag :value="row.status" kind="announcement" /></template>
          </el-table-column>
          <el-table-column label="发布时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.publishedAt) }}</template>
          </el-table-column>
          <el-table-column label="最后更新" width="170">
            <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="230" align="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPreview(row)">预览</el-button>
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button
                link
                :type="row.status === 'PUBLISHED' ? 'warning' : 'success'"
                @click="toggleStatus(row)"
              >
                {{ row.status === 'PUBLISHED' ? '下线' : '发布' }}
              </el-button>
              <el-button link type="danger" @click="remove(row)">删除</el-button>
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
        下线后用户端不再展示该内容；删除为物理删除，请确认无需保留历史。
      </div>
    </section>

    <el-drawer v-model="drawerVisible" :title="editing ? '编辑公告' : '新增公告'" size="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="50" show-word-limit placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio-button value="NOTICE">系统公告</el-radio-button>
            <el-radio-button value="KNOWLEDGE">环保科普</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="封面图（可选）">
          <UploadImage v-model="form.coverUrl" scene="ANNOUNCEMENT" />
        </el-form-item>
        <el-form-item label="正文（纯文本，保留换行）" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            maxlength="5000"
            show-word-limit
            placeholder="请输入公告正文，支持换行"
          />
        </el-form-item>
        <el-form-item label="发布状态">
          <el-radio-group v-model="form.status">
            <el-radio value="PUBLISHED">立即发布</el-radio>
            <el-radio value="OFFLINE">保存为下线</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          {{ editing ? '保存修改' : '创建公告' }}
        </el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="previewVisible" title="公告预览" width="560px" align-center>
      <article v-if="previewItem" class="preview">
        <span class="preview__type">{{ previewItem.type === 'NOTICE' ? '系统公告' : '环保科普' }}</span>
        <h2>{{ previewItem.title }}</h2>
        <p class="preview__meta">{{ formatDateTime(previewItem.publishedAt || previewItem.createdAt) }}</p>
        <img v-if="previewItem.coverUrl" class="preview__cover" :src="previewItem.coverUrl" alt="" />
        <p class="preview__content">{{ previewItem.content }}</p>
      </article>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button type="primary" @click="editFromPreview">编辑</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import UploadImage from '@/components/UploadImage.vue'
import {
  createAnnouncement,
  deleteAnnouncement,
  listAdminAnnouncements,
  updateAnnouncement,
  updateAnnouncementStatus
} from '@/api/announcement'
import { formatDateTime } from '@/utils/format'

const announcements = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const filters = reactive({ keyword: '', type: '', status: '' })

const drawerVisible = ref(false)
const editing = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref(null)

const previewVisible = ref(false)
const previewItem = ref(null)

const form = reactive({
  title: '',
  type: 'NOTICE',
  content: '',
  coverUrl: '',
  status: 'PUBLISHED'
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const data = await listAdminAnnouncements({
      page: page.value,
      size: 20,
      keyword: filters.keyword.trim() || undefined,
      type: filters.type || undefined,
      status: filters.status || undefined
    })
    announcements.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 1
  load()
}

function resetFilters() {
  filters.keyword = ''
  filters.type = ''
  filters.status = ''
  applyFilters()
}

function openCreate() {
  editing.value = false
  editingId.value = null
  Object.assign(form, { title: '', type: 'NOTICE', content: '', coverUrl: '', status: 'PUBLISHED' })
  formRef.value?.clearValidate()
  drawerVisible.value = true
}

function openEdit(row) {
  editing.value = true
  editingId.value = row.announcementId
  Object.assign(form, {
    title: row.title,
    type: row.type,
    content: row.content,
    coverUrl: row.coverUrl || '',
    status: row.status
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
    const payload = {
      title: form.title,
      type: form.type,
      content: form.content,
      coverUrl: form.coverUrl || null,
      status: form.status
    }
    if (editing.value) {
      await updateAnnouncement(editingId.value, payload)
      ElMessage.success('公告已更新')
    } else {
      await createAnnouncement(payload)
      ElMessage.success(form.status === 'PUBLISHED' ? '公告已发布' : '公告已保存为下线')
    }
    drawerVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 'PUBLISHED' ? 'OFFLINE' : 'PUBLISHED'
  const label = next === 'OFFLINE' ? '下线' : '发布'
  try {
    await ElMessageBox.confirm(
      next === 'OFFLINE'
        ? `确认下线「${row.title}」？下线后用户端不再展示该内容。`
        : `确认发布「${row.title}」？发布后用户端可立即看到该内容。`,
      `${label}公告`,
      { confirmButtonText: label, cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await updateAnnouncementStatus(row.announcementId, { status: next })
  ElMessage.success(`已${label}「${row.title}」`)
  await load()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除公告「${row.title}」？物理删除后不可恢复。`,
      '删除公告',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await deleteAnnouncement(row.announcementId)
  ElMessage.success('公告已删除')
  await load()
}

function openPreview(row) {
  previewItem.value = row
  previewVisible.value = true
}

function editFromPreview() {
  previewVisible.value = false
  openEdit(previewItem.value)
}

onMounted(load)
</script>

<style scoped>
.title-cell {
  display: flex;
  align-items: center;
  gap: 12px;
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

.preview {
  max-height: 60vh;
  overflow: auto;
  padding-right: 6px;
}

.preview__type {
  display: inline-block;
  padding: 2px 9px;
  border-radius: 999px;
  background: var(--brand-50);
  color: var(--brand-700);
  font-size: 10.5px;
  font-weight: 600;
}

.preview h2 {
  margin-top: 10px;
  font-size: 20px;
  font-weight: 650;
  line-height: 1.4;
}

.preview__meta {
  margin-top: 6px;
  color: var(--ink-400);
  font-size: 11.5px;
}

.preview__cover {
  width: 100%;
  max-height: 220px;
  object-fit: cover;
  border-radius: 10px;
  margin-top: 16px;
}

.preview__content {
  margin-top: 16px;
  color: var(--ink-700);
  font-size: 13.5px;
  line-height: 1.9;
  white-space: pre-wrap;
}
</style>
