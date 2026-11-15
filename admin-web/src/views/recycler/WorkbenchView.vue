<template>
  <div class="page">
    <PageHeader
      title="工作台"
      :description="`你好，${auth.displayName}。${todayText}，站点：${workbench?.siteName || auth.siteName || '未分配'}`"
    >
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新数据</el-button>
        <el-button type="primary" :icon="Aim" @click="router.push('/workbench/identify')">
          扫码识别用户
        </el-button>
      </template>
    </PageHeader>

    <div class="stat-grid">
      <StatCard
        label="今日回收重量"
        :value="workbench ? workbench.todayWeight.toFixed(2) : '—'"
        unit="kg"
        icon="ScaleToOriginal"
        tone="green"
        hint="仅统计正常状态的流水"
        :loading="loading"
      />
      <StatCard
        label="今日发放积分"
        :value="workbench ? formatNumber(workbench.todayPoints) : '—'"
        unit="分"
        icon="Coin"
        tone="amber"
        hint="由系统按类别系数自动计算"
        :loading="loading"
      />
      <StatCard
        label="今日投递笔数"
        :value="workbench ? workbench.todayDeliveryCount : '—'"
        unit="笔"
        icon="Document"
        tone="blue"
        hint="点击进入我的流水"
        clickable
        :loading="loading"
        @click="router.push('/workbench/records')"
      />
      <StatCard
        label="今日核销笔数"
        :value="workbench ? workbench.todayRedeemCount : '—'"
        unit="笔"
        icon="Tickets"
        tone="plum"
        hint="兑换订单核销完成后计入"
        clickable
        :loading="loading"
        @click="router.push('/workbench/redeem')"
      />
    </div>

    <div class="workbench-grid">
      <section class="action-card">
        <div class="action-card__glow" aria-hidden="true" />
        <div class="action-card__main">
          <span class="action-card__icon">
            <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.6">
              <path d="M4 8V6a2 2 0 0 1 2-2h2M16 4h2a2 2 0 0 1 2 2v2M20 16v2a2 2 0 0 1-2 2h-2M8 20H6a2 2 0 0 1-2-2v-2" stroke-linecap="round" />
              <path d="M4 12h16" stroke-linecap="round" />
            </svg>
          </span>
          <div>
            <h2>识别用户，开始投递</h2>
            <p>扫描用户二维码，或输入手机号 / 6 位用户编号。</p>
          </div>
        </div>
        <div class="action-card__foot">
          <el-button type="primary" size="large" :icon="Aim" @click="router.push('/workbench/identify')">
            识别用户
          </el-button>
          <el-button size="large" plain class="ghost" @click="router.push('/workbench/delivery')">
            继续上次登记
          </el-button>
        </div>
      </section>

      <section class="panel trend-card">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">近 7 日回收重量</h3>
            <p class="panel-sub">仅统计本人经手且状态正常的流水</p>
          </div>
          <div class="rank" v-if="workbench?.monthRank">
            <span>本月站点排名</span>
            <strong>第 {{ workbench.monthRank }} 名</strong>
          </div>
        </div>
        <div class="trend-body">
          <BaseChart v-if="trendOption" :option="trendOption" height="220px" />
          <el-skeleton v-else animated :rows="4" />
        </div>
      </section>
    </div>

    <div class="lower-grid">
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">当前积分规则</h3>
            <p class="panel-sub">由管理员配置，仅供回收员解释积分用</p>
          </div>
          <el-button text type="primary" @click="router.push('/workbench/rules')">查看详情</el-button>
        </div>
        <div class="rule-strip">
          <div v-for="category in categories" :key="category.categoryId" class="rule-chip">
            <span class="rule-chip__name">{{ category.name }}</span>
            <span class="rule-chip__value num">{{ category.pointsPerKg }}<small>分 / kg</small></span>
          </div>
          <el-empty v-if="!loading && !categories.length" description="暂无启用中的类别" :image-size="60" />
        </div>
      </section>

      <section class="panel quick-card">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">快捷入口</h3>
            <p class="panel-sub">高频操作直达</p>
          </div>
        </div>
        <div class="quick-list">
          <button type="button" class="quick-item" @click="router.push('/workbench/redeem')">
            <span class="quick-item__icon"><el-icon><Ticket /></el-icon></span>
            <span><strong>兑换核销</strong><small>输入 8 位核销码，预览后确认</small></span>
            <el-icon class="quick-item__arrow"><ArrowRight /></el-icon>
          </button>
          <button type="button" class="quick-item" @click="router.push('/workbench/records')">
            <span class="quick-item__icon"><el-icon><List /></el-icon></span>
            <span><strong>我的流水</strong><small>查询本人经手记录，5 分钟内可撤销</small></span>
            <el-icon class="quick-item__arrow"><ArrowRight /></el-icon>
          </button>
          <button type="button" class="quick-item" @click="router.push('/workbench/delivery')">
            <span class="quick-item__icon"><el-icon><EditPen /></el-icon></span>
            <span><strong>投递登记</strong><small>选择类别、输入重量，自动计算积分</small></span>
            <el-icon class="quick-item__arrow"><ArrowRight /></el-icon>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Aim, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import BaseChart from '@/components/BaseChart.vue'
import { useAuthStore } from '@/stores/auth'
import { getWorkbench } from '@/api/stats'
import { listCategories } from '@/api/category'
import { formatNumber } from '@/utils/format'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const workbench = ref(null)
const categories = ref([])

const todayText = computed(() =>
  new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())
)

const trendOption = computed(() => {
  if (!workbench.value) return null
  const trend = workbench.value.weekTrend || []
  return {
    grid: { left: 6, right: 10, top: 18, bottom: 0, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (value) => `${value} kg`
    },
    xAxis: {
      type: 'category',
      data: trend.map((item) => item.date.slice(5)),
      axisLine: { lineStyle: { color: '#e5eae6' } },
      axisTick: { show: false },
      axisLabel: { color: '#8b988f', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#eef2ef' } },
      axisLabel: { color: '#8b988f', fontSize: 11 }
    },
    series: [
      {
        type: 'bar',
        data: trend.map((item) => item.weight),
        barWidth: 20,
        itemStyle: {
          borderRadius: [5, 5, 0, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#3fcf8e' },
              { offset: 1, color: '#16794c' }
            ]
          }
        }
      }
    ]
  }
})

async function load() {
  loading.value = true
  try {
    const [wb, cats] = await Promise.all([getWorkbench(), listCategories({ enabledOnly: true })])
    workbench.value = wb
    categories.value = cats.filter((item) => item.status === 'ENABLED')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.workbench-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.5fr);
  gap: 20px;
  align-items: stretch;
}

.action-card {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 26px;
  min-height: 280px;
  padding: 28px;
  border-radius: var(--radius-lg);
  background:
    radial-gradient(600px 300px at 100% 0%, rgba(92, 221, 158, 0.22), transparent 60%),
    linear-gradient(150deg, #12603d 0%, #0d3a26 100%);
  color: #fff;
  overflow: hidden;
  box-shadow: 0 18px 40px -30px rgba(11, 42, 28, 0.9);
}

.action-card__glow {
  position: absolute;
  right: -70px;
  bottom: -90px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.action-card__main {
  display: flex;
  gap: 16px;
  position: relative;
}

.action-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.1);
  color: #7cebb5;
  flex: none;
}

.action-card h2 {
  font-size: 19px;
  font-weight: 650;
  letter-spacing: -0.01em;
}

.action-card p {
  margin-top: 6px;
  color: rgba(226, 240, 232, 0.62);
  font-size: 12.5px;
  line-height: 1.7;
}

.action-card__foot {
  display: flex;
  gap: 10px;
  position: relative;
}

.action-card__foot .el-button {
  height: 44px;
  border-radius: 10px;
}

.action-card__foot :deep(.el-button--primary) {
  background: #3fcf8e;
  border-color: #3fcf8e;
  color: #0b2a1c;
}

.action-card__foot :deep(.el-button--primary:hover) {
  background: #5cdd9e;
  border-color: #5cdd9e;
  color: #0b2a1c;
}

.ghost {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.18);
  color: rgba(255, 255, 255, 0.8);
}

.ghost:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.28);
  color: #fff;
}

.trend-card {
  min-height: 280px;
}

.trend-body {
  padding: 12px 18px 10px;
}

.rank {
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 999px;
  background: var(--amber-50);
  color: var(--amber-700);
  font-size: 11.5px;
}

.rank strong {
  font-size: 12.5px;
}

.lower-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.65fr);
  gap: 20px;
  align-items: start;
}

.rule-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 18px 22px 22px;
}

.rule-chip {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border: 1px solid var(--line);
  border-radius: 11px;
  background: #fbfdfb;
}

.rule-chip__name {
  color: var(--ink-700);
  font-size: 13px;
  font-weight: 550;
}

.rule-chip__value {
  color: var(--brand-700);
  font-size: 16px;
  font-weight: 650;
}

.rule-chip__value small {
  margin-left: 2px;
  color: var(--ink-400);
  font-size: 10.5px;
  font-weight: 500;
}

.quick-list {
  display: flex;
  flex-direction: column;
  padding: 8px 14px 14px;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 10px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background 0.16s ease;
}

.quick-item:hover {
  background: var(--brand-50);
}

.quick-item__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--brand-100);
  color: var(--brand-700);
  font-size: 16px;
  flex: none;
}

.quick-item strong {
  display: block;
  color: var(--ink-900);
  font-size: 13px;
  font-weight: 600;
}

.quick-item small {
  display: block;
  margin-top: 1px;
  color: var(--ink-500);
  font-size: 11.5px;
}

.quick-item__arrow {
  margin-left: auto;
  color: var(--ink-400);
  font-size: 13px;
}

@media (max-width: 1240px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workbench-grid,
  .lower-grid {
    grid-template-columns: 1fr;
  }
}
</style>
