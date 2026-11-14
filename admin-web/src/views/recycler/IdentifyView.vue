<template>
  <div class="page">
    <PageHeader
      title="识别用户"
      description="扫描用户身份二维码，或输入手机号 / 6 位用户编号。识别成功后可进入投递登记。"
    >
      <template #actions>
        <el-button :icon="Refresh" @click="reset">重新识别</el-button>
      </template>
    </PageHeader>

    <div class="identify-grid">
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">选择识别方式</h3>
            <p class="panel-sub">推荐使用扫描枪，扫码后自动回车</p>
          </div>
        </div>

        <div class="identify-body">
          <div class="mode-switch">
            <button
              v-for="mode in modes"
              :key="mode.key"
              type="button"
              :class="{ active: activeTab === mode.key }"
              @click="switchMode(mode.key)"
            >
              <el-icon><component :is="mode.icon" /></el-icon>
              {{ mode.label }}
            </button>
          </div>

          <div class="input-zone">
            <template v-if="activeTab === 'qr'">
              <div class="scan-frame">
                <span class="scan-frame__corners" aria-hidden="true" />
                <el-icon class="scan-frame__icon"><FullScreen /></el-icon>
                <p>将光标置于下方输入框，扫描用户二维码</p>
                <el-input
                  v-model="values.qr"
                  size="large"
                  placeholder="扫描后自动填入，或粘贴 NUAA-RECYCLE:U:… 内容"
                  clearable
                  @keyup.enter="lookup"
                />
              </div>
            </template>

            <template v-else-if="activeTab === 'phone'">
              <el-form-item label="手机号" class="single-field">
                <el-input
                  v-model="values.phone"
                  size="large"
                  maxlength="11"
                  placeholder="请输入用户 11 位手机号"
                  :prefix-icon="Phone"
                  clearable
                  @keyup.enter="lookup"
                />
              </el-form-item>
            </template>

            <template v-else>
              <el-form-item label="6 位用户编号" class="single-field">
                <el-input
                  v-model="values.code"
                  size="large"
                  maxlength="6"
                  placeholder="例如 100001"
                  :prefix-icon="Postcard"
                  clearable
                  @keyup.enter="lookup"
                />
              </el-form-item>
            </template>

            <div class="input-actions">
              <el-button type="primary" size="large" :loading="loading" @click="lookup">
                识别用户
              </el-button>
              <span class="muted">识别结果只显示脱敏手机号与积分，不返回地址等隐私字段。</span>
            </div>
          </div>

          <el-alert
            v-if="errorMessage"
            :title="errorMessage"
            type="error"
            show-icon
            :closable="false"
            class="identify-error"
          />

          <div v-if="recentUsers.length" class="recent">
            <p class="recent__title">最近识别</p>
            <div class="recent__list">
              <button
                v-for="item in recentUsers"
                :key="item.userId"
                type="button"
                class="recent__item"
                @click="selectUser(item)"
              >
                <span class="avatar-initial">{{ item.nickname.slice(0, 1) }}</span>
                <span>
                  <strong>{{ item.nickname }}</strong>
                  <small>{{ item.userCode }} · {{ item.phoneMasked }}</small>
                </span>
              </button>
            </div>
          </div>
        </div>
      </section>

      <section class="panel result-panel">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">识别结果</h3>
            <p class="panel-sub">确认用户身份后再录入投递</p>
          </div>
        </div>

        <div v-if="user" class="user-card">
          <div class="user-card__head">
            <span class="user-avatar">{{ user.nickname.slice(0, 1) }}</span>
            <div class="user-card__name">
              <h2>{{ user.nickname }}</h2>
              <p class="mono">编号 {{ user.userCode }}</p>
            </div>
            <StatusTag :value="user.status" kind="user" />
          </div>

          <div class="user-metrics">
            <div>
              <span class="muted">手机号</span>
              <strong class="mono">{{ user.phoneMasked }}</strong>
            </div>
            <div>
              <span class="muted">当前积分</span>
              <strong class="points num">{{ formatNumber(user.pointsBalance) }}<small>分</small></strong>
            </div>
          </div>

          <el-alert
            type="success"
            :closable="false"
            show-icon
            title="身份识别成功"
            description="请核对用户姓名与编号，确认无误后开始投递登记。"
          />

          <div class="user-card__actions">
            <el-button type="primary" size="large" :icon="EditPen" @click="startDelivery">
              开始投递登记
            </el-button>
            <el-button size="large" @click="reset">换一个用户</el-button>
          </div>
        </div>

        <div v-else class="result-empty">
          <span class="result-empty__icon"><el-icon><Aim /></el-icon></span>
          <h4>等待识别用户</h4>
          <p>可扫描二维码，或输入手机号、6 位用户编号。</p>
          <p class="tip">浏览器摄像头扫码需要 HTTPS；演示环境已退化为扫描枪 / 手动输入。</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Aim, EditPen, Phone, Postcard, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { lookupUser } from '@/api/delivery'
import { formatNumber } from '@/utils/format'

const router = useRouter()

const modes = [
  { key: 'qr', label: '扫码 / 扫描枪', icon: 'FullScreen' },
  { key: 'phone', label: '手机号', icon: 'Phone' },
  { key: 'code', label: '用户编号', icon: 'Postcard' }
]

const activeTab = ref('qr')
const values = reactive({ qr: '', phone: '', code: '' })
const loading = ref(false)
const user = ref(null)
const errorMessage = ref('')
const recentUsers = ref([])

function switchMode(key) {
  activeTab.value = key
  errorMessage.value = ''
}

function buildParams() {
  if (activeTab.value === 'qr') {
    const qrContent = values.qr.trim()
    if (!qrContent) throw new Error('请扫描或输入二维码内容')
    return { qrContent }
  }
  if (activeTab.value === 'phone') {
    const keyword = values.phone.trim()
    if (!/^1\d{10}$/.test(keyword)) throw new Error('请输入正确的 11 位手机号')
    return { keyword }
  }
  const keyword = values.code.trim()
  if (!/^\d{6}$/.test(keyword)) throw new Error('请输入 6 位数字用户编号')
  return { keyword }
}

async function lookup() {
  if (loading.value) return
  errorMessage.value = ''
  let params
  try {
    params = buildParams()
  } catch (error) {
    errorMessage.value = error.message
    return
  }

  loading.value = true
  try {
    const data = await lookupUser(params)
    selectUser(data)
  } catch (error) {
    user.value = null
    errorMessage.value = error?.message || '识别失败，请重试'
  } finally {
    loading.value = false
  }
}

function selectUser(data) {
  user.value = data
  errorMessage.value = ''
  recentUsers.value = [data, ...recentUsers.value.filter((item) => item.userId !== data.userId)].slice(0, 4)
}

function startDelivery() {
  if (!user.value) return
  router.push({
    path: '/workbench/delivery',
    query: {
      userId: user.value.userId,
      nickname: user.value.nickname,
      userCode: user.value.userCode,
      phoneMasked: user.value.phoneMasked,
      pointsBalance: user.value.pointsBalance
    }
  })
}

function reset() {
  user.value = null
  errorMessage.value = ''
  values.qr = ''
  values.phone = ''
  values.code = ''
}
</script>

<style scoped>
.identify-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(340px, 0.85fr);
  gap: 20px;
  align-items: start;
}

.identify-body {
  padding: 20px 22px 24px;
}

.mode-switch {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  padding: 5px;
  background: #edf2ef;
  border-radius: 12px;
  margin-bottom: 20px;
}

.mode-switch button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 40px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: var(--ink-500);
  font-family: inherit;
  font-size: 13px;
  font-weight: 550;
  cursor: pointer;
  transition: all 0.18s ease;
}

.mode-switch button.active {
  background: #fff;
  color: var(--brand-700);
  box-shadow: 0 2px 8px rgba(16, 36, 25, 0.08);
}

.scan-frame {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 34px 28px 26px;
  border: 1px dashed var(--line-strong);
  border-radius: 14px;
  background: #fbfdfb;
  margin-bottom: 18px;
}

.scan-frame__corners {
  position: absolute;
  inset: 14px;
  pointer-events: none;
  background:
    linear-gradient(var(--brand-500), var(--brand-500)) left top / 16px 2px no-repeat,
    linear-gradient(var(--brand-500), var(--brand-500)) left top / 2px 16px no-repeat,
    linear-gradient(var(--brand-500), var(--brand-500)) right top / 16px 2px no-repeat,
    linear-gradient(var(--brand-500), var(--brand-500)) right top / 2px 16px no-repeat,
    linear-gradient(var(--brand-500), var(--brand-500)) left bottom / 16px 2px no-repeat,
    linear-gradient(var(--brand-500), var(--brand-500)) left bottom / 2px 16px no-repeat,
    linear-gradient(var(--brand-500), var(--brand-500)) right bottom / 16px 2px no-repeat,
    linear-gradient(var(--brand-500), var(--brand-500)) right bottom / 2px 16px no-repeat;
  opacity: 0.55;
}

.scan-frame__icon {
  font-size: 34px;
  color: var(--brand-600);
}

.scan-frame p {
  color: var(--ink-500);
  font-size: 12.5px;
  text-align: center;
  margin-bottom: 4px;
}

.scan-frame :deep(.el-input) {
  max-width: 460px;
}

.single-field {
  max-width: 460px;
  margin-bottom: 18px;
}

.input-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.input-actions .el-button {
  min-width: 128px;
  height: 44px;
}

.input-actions .muted {
  font-size: 11.5px;
}

.identify-error {
  margin-top: 18px;
}

.recent {
  margin-top: 26px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}

.recent__title {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 10px;
}

.recent__list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.recent__item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 7px 12px 7px 8px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fff;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.16s ease, background 0.16s ease;
}

.recent__item:hover {
  border-color: var(--brand-500);
  background: var(--brand-50);
}

.recent__item strong {
  display: block;
  color: var(--ink-900);
  font-size: 12.5px;
}

.recent__item small {
  display: block;
  color: var(--ink-500);
  font-size: 10.5px;
}

.result-panel {
  min-height: 420px;
}

.user-card {
  padding: 24px 22px;
}

.user-card__head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--line);
}

.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  border-radius: 16px;
  background: linear-gradient(135deg, #2fbd7c, #16794c);
  color: #fff;
  font-size: 22px;
  font-weight: 650;
  flex: none;
}

.user-card__name {
  flex: 1;
  min-width: 0;
}

.user-card__name h2 {
  font-size: 18px;
  font-weight: 650;
}

.user-card__name p {
  margin-top: 2px;
  color: var(--ink-500);
  font-size: 11.5px;
}

.user-metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  padding: 20px 0;
}

.user-metrics > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 11px;
  background: #f8faf9;
  border: 1px solid var(--line);
}

.user-metrics span {
  font-size: 11px;
}

.user-metrics strong {
  font-size: 14px;
}

.user-metrics .points {
  color: var(--brand-700);
  font-size: 20px;
  font-weight: 650;
}

.user-metrics .points small {
  margin-left: 3px;
  color: var(--ink-400);
  font-size: 10.5px;
  font-weight: 500;
}

.user-card__actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.user-card__actions .el-button {
  height: 44px;
}

.result-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 56px 30px;
}

.result-empty__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 66px;
  height: 66px;
  border-radius: 50%;
  background: var(--brand-50);
  border: 1px dashed #b9dcc9;
  color: var(--brand-600);
  font-size: 28px;
  margin-bottom: 18px;
}

.result-empty h4 {
  font-size: 15px;
  font-weight: 600;
}

.result-empty p {
  margin-top: 6px;
  color: var(--ink-500);
  font-size: 12.5px;
}

.result-empty .tip {
  margin-top: 14px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f8faf9;
  color: var(--ink-400);
  font-size: 11px;
  max-width: 300px;
}

@media (max-width: 1100px) {
  .identify-grid {
    grid-template-columns: 1fr;
  }
}
</style>
