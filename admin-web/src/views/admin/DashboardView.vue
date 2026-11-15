<template>
  <div class="page">
    <PageHeader title="数据看板" description="全局回收、积分与兑换概览。数据来自统计聚合接口，进入页面时拉取。">
      <template #actions>
        <div class="auto-refresh">
          <span>自动刷新</span>
          <el-switch v-model="autoRefresh" size="small" />
          <small v-if="autoRefresh">每 30 秒</small>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </template>
    </PageHeader>

    <div class="stat-grid">
      <StatCard
        label="累计回收重量"
        :value="overview ? formatWeight(overview.totalWeight) : '—'"
        unit="kg"
        icon="ScaleToOriginal"
        tone="green"
        hint="仅统计状态为正常的流水"
        :loading="loading"
      />
      <StatCard
        label="累计发放积分"
        :value="overview ? formatNumber(overview.totalPointsIssued) : '—'"
        unit="分"
        icon="Coin"
        tone="amber"
        hint="回收员登记后由系统自动发放"
        :loading="loading"
      />
      <StatCard
        label="累计消耗积分"
        :value="overview ? formatNumber(overview.totalPointsRedeemed) : '—'"
        unit="分"
        icon="Tickets"
        tone="blue"
        hint="仅统计已核销的兑换订单"
        :loading="loading"
      />
      <StatCard
        label="注册用户"
        :value="overview ? overview.userCount : '—'"
        unit="人"
        icon="User"
        tone="plum"
        :hint="overview ? `已通过审核回收员 ${overview.recyclerCount} 人` : ''"
        :loading="loading"
      />
      <StatCard
        label="已完成兑换"
        :value="overview ? overview.completedOrderCount : '—'"
        unit="笔"
        icon="CircleCheck"
        tone="slate"
        hint="核销完成且不可撤销"
        :loading="loading"
        clickable
        @click="router.push('/audit/orders')"
      />
    </div>

    <div class="dash-grid">
      <section class="panel trend-panel">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">回收趋势</h3>
            <p class="panel-sub">每日回收重量与发放积分</p>
          </div>
          <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
          </el-radio-group>
        </div>
        <div class="chart-body">
          <el-skeleton v-if="!trend.length && loading" animated :rows="5" />
          <BaseChart v-else :option="trendOption" height="300px" />
        </div>
      </section>

      <section class="panel todo-panel">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">待办提醒</h3>
            <p class="panel-sub">需要管理员处理的事项</p>
          </div>
          <span class="todo-total" :class="{ 'is-clear': todoTotal === 0 }">
            {{ todoTotal === 0 ? '已全部处理' : `${todoTotal} 项待处理` }}
          </span>
        </div>

        <div class="todo-list">
          <button
            v-for="item in todos"
            :key="item.key"
            type="button"
            class="todo-item"
            :class="{ 'is-empty': !item.count }"
            @click="router.push(item.path)"
          >
            <span class="todo-item__icon" :class="`tone--${item.tone}`">
              <el-icon><component :is="item.icon" /></el-icon>
            </span>
            <span class="todo-item__body">
              <strong>{{ item.title }}</strong>
              <small>{{ item.desc }}</small>
            </span>
            <span class="todo-item__count num">{{ item.count }}</span>
            <el-icon class="todo-item__arrow"><ArrowRight /></el-icon>
          </button>
        </div>

        <div class="todo-foot">
          <el-icon><InfoFilled /></el-icon>
          待办数量来自概览接口，处理完成后刷新即可更新。
        </div>
      </section>
    </div>

    <div class="dash-grid dash-grid--half">
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">分类回收占比</h3>
            <p class="panel-sub">按累计回收重量计算</p>
          </div>
          <el-button text type="primary" @click="router.push('/config/categories')">类别配置</el-button>
        </div>
        <div class="chart-body">
          <el-skeleton v-if="!categoryStats.length && loading" animated :rows="5" />
          <BaseChart v-else :option="pieOption" height="280px" />
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">TOP 排行</h3>
            <p class="panel-sub">{{ topType === 'PRODUCT' ? '按已核销数量排序' : '按累计投递重量排序' }}</p>
          </div>
          <el-radio-group v-model="topType" size="small" @change="loadTop">
            <el-radio-button value="PRODUCT">热门商品</el-radio-button>
            <el-radio-button value="USER">活跃用户</el-radio-button>
          </el-radio-group>
        </div>
        <div class="top-body">
          <div v-if="!topList.length" class="top-empty">
            <el-empty description="暂无排行数据" :image-size="70" />
          </div>
          <div v-for="(item, index) in topList" :key="item.id" class="top-row">
            <span class="top-rank" :class="`rank-${index + 1}`">{{ index + 1 }}</span>
            <div class="top-main">
              <div class="top-line">
                <strong>{{ item.name }}</strong>
                <span class="num">
                  {{ topType === 'PRODUCT' ? `${item.value} 件` : `${formatWeight(item.value)} kg` }}
                </span>
              </div>
              <div class="top-bar">
                <span :style="{ width: `${barWidth(item.value)}%` }" />
              </div>
              <small>{{ topType === 'PRODUCT' ? `消耗积分 ${formatNumber(item.extra)} 分` : `获得积分 ${formatNumber(item.extra)} 分` }}</small>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, InfoFilled, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import BaseChart from '@/components/BaseChart.vue'
import { getCategoryStats, getOverview, getTop, getTrend } from '@/api/stats'
import { formatNumber, formatWeight } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const overview = ref(null)
const categoryStats = ref([])
const trend = ref([])
const topList = ref([])
const trendDays = ref(7)
const topType = ref('PRODUCT')
const autoRefresh = ref(false)
let timer = null

const todoTotal = computed(() => {
  if (!overview.value) return 0
  return (
    overview.value.pendingRecyclerCount +
    overview.value.pendingAppealCount +
    overview.value.expiredPendingOrderCount
  )
})

const todos = computed(() => [
  {
    key: 'recycler',
    title: '待审核回收员',
    desc: '新的回收员注册申请等待通过或驳回',
    count: overview.value?.pendingRecyclerCount ?? 0,
    path: '/accounts/recyclers',
    icon: 'UserFilled',
    tone: 'amber'
  },
  {
    key: 'appeal',
    title: '待处理申诉',
    desc: '用户对投递或兑换结果提出申诉',
    count: overview.value?.pendingAppealCount ?? 0,
    path: '/audit/appeals',
    icon: 'ChatDotRound',
    tone: 'plum'
  },
  {
    key: 'order',
    title: '超期未核销订单',
    desc: '核销码已过期，建议取消并退回积分',
    count: overview.value?.expiredPendingOrderCount ?? 0,
    path: '/audit/orders',
    icon: 'Clock',
    tone: 'blue'
  }
])

const trendOption = computed(() => ({
  grid: { left: 8, right: 16, top: 42, bottom: 4, containLabel: true },
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  legend: {
    right: 8,
    top: 4,
    itemWidth: 10,
    itemHeight: 10,
    icon: 'roundRect',
    textStyle: { color: '#6b7a72', fontSize: 11 }
  },
  xAxis: {
    type: 'category',
    data: trend.value.map((item) => item.date.slice(5)),
    axisLine: { lineStyle: { color: '#e5eae6' } },
    axisTick: { show: false },
    axisLabel: { color: '#8b988f', fontSize: 11, interval: trendDays.value === 30 ? 4 : 0 }
  },
  yAxis: [
    {
      type: 'value',
      name: 'kg',
      nameTextStyle: { color: '#8b988f', fontSize: 10 },
      splitLine: { lineStyle: { color: '#eef2ef' } },
      axisLabel: { color: '#8b988f', fontSize: 11 }
    },
    {
      type: 'value',
      name: '积分',
      nameTextStyle: { color: '#8b988f', fontSize: 10 },
      splitLine: { show: false },
      axisLabel: { color: '#8b988f', fontSize: 11 }
    }
  ],
  series: [
    {
      name: '回收重量',
      type: 'bar',
      data: trend.value.map((item) => item.weight),
      barMaxWidth: 18,
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
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
    },
    {
      name: '发放积分',
      type: 'line',
      yAxisIndex: 1,
      data: trend.value.map((item) => item.points),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2, color: '#d99a1f' },
      itemStyle: { color: '#d99a1f' }
    }
  ]
}))

const pieOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: (params) => `${params.name}<br/>${params.value} kg · ${params.percent}%`
  },
  legend: {
    bottom: 0,
    icon: 'circle',
    itemWidth: 8,
    itemHeight: 8,
    itemGap: 16,
    textStyle: { color: '#6b7a72', fontSize: 11 }
  },
  title: {
    text: formatWeight(categoryStats.value.reduce((sum, item) => sum + item.weight, 0)),
    subtext: '累计 kg',
    left: 'center',
    top: '34%',
    textStyle: { color: '#15211b', fontSize: 22, fontWeight: 650 },
    subtextStyle: { color: '#8b988f', fontSize: 11 }
  },
  series: [
    {
      type: 'pie',
      radius: ['52%', '72%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
      label: { show: false },
      emphasis: { scale: true, scaleSize: 4 },
      data: categoryStats.value.map((item, index) => ({
        name: item.categoryName,
        value: item.weight,
        itemStyle: { color: ['#16794c', '#3f9d74', '#7fba9d', '#c9a227', '#8a9b91', '#3b6e91'][index % 6] }
      }))
    }
  ]
}))

function barWidth(value) {
  const max = Math.max(...topList.value.map((item) => Number(item.value)), 1)
  return Math.max(6, Math.round((Number(value) / max) * 100))
}

async function loadTrend() {
  trend.value = await getTrend(trendDays.value)
}

async function loadTop() {
  topList.value = await getTop(topType.value, 5)
}

async function loadAll() {
  loading.value = true
  try {
    const [ov, cats, days, top] = await Promise.all([
      getOverview(),
      getCategoryStats(),
      getTrend(trendDays.value),
      getTop(topType.value, 5)
    ])
    overview.value = ov
    categoryStats.value = cats
    trend.value = days
    topList.value = top
  } finally {
    loading.value = false
  }
}

function startTimer() {
  stopTimer()
  timer = setInterval(loadAll, 30000)
}

function stopTimer() {
  if (timer) clearInterval(timer)
  timer = null
}

watch(autoRefresh, (value) => (value ? startTimer() : stopTimer()))

onMounted(loadAll)
onBeforeUnmount(stopTimer)
</script>

<style scoped>
.auto-refresh {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--ink-500);
  font-size: 12px;
}

.auto-refresh small {
  color: var(--ink-400);
  font-size: 10.5px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.dash-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(340px, 0.8fr);
  gap: 20px;
  align-items: stretch;
}

.dash-grid--half {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.chart-body {
  padding: 16px 20px 14px;
}

.todo-total {
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--amber-50);
  color: var(--amber-700);
  font-size: 11px;
  font-weight: 600;
}

.todo-total.is-clear {
  background: var(--brand-50);
  color: var(--brand-600);
}

.todo-list {
  display: flex;
  flex-direction: column;
  padding: 10px 14px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 13px 10px;
  border: 0;
  border-radius: 11px;
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background 0.16s ease;
}

.todo-item:hover {
  background: var(--brand-50);
}

.todo-item__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  font-size: 17px;
  flex: none;
}

.tone--amber {
  background: var(--amber-50);
  color: var(--amber-700);
}

.tone--plum {
  background: #f3ecf6;
  color: #7a4c8c;
}

.tone--blue {
  background: var(--blue-50);
  color: var(--blue-600);
}

.todo-item__body {
  flex: 1;
  min-width: 0;
}

.todo-item__body strong {
  display: block;
  color: var(--ink-900);
  font-size: 13px;
  font-weight: 600;
}

.todo-item__body small {
  display: block;
  margin-top: 1px;
  color: var(--ink-500);
  font-size: 11px;
}

.todo-item__count {
  min-width: 28px;
  color: var(--amber-700);
  font-size: 20px;
  font-weight: 650;
  text-align: right;
}

.todo-item.is-empty .todo-item__count {
  color: var(--ink-400);
  font-weight: 500;
}

.todo-item__arrow {
  color: var(--ink-400);
  font-size: 12px;
}

.todo-foot {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: auto 22px 20px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
  color: var(--ink-400);
  font-size: 10.5px;
}

.top-body {
  padding: 14px 22px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.top-empty {
  padding: 25px 0;
}

.top-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.top-rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 7px;
  background: #f1f5f2;
  color: var(--ink-500);
  font-family: var(--font-mono);
  font-size: 11.5px;
  font-weight: 700;
  flex: none;
}

.top-rank.rank-1 {
  background: #fbf1dc;
  color: #a16207;
}

.top-rank.rank-2 {
  background: #eef2ef;
  color: #4c5f55;
}

.top-rank.rank-3 {
  background: #f6ede5;
  color: #9a6a3f;
}

.top-main {
  flex: 1;
  min-width: 0;
}

.top-line {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.top-line strong {
  color: var(--ink-900);
  font-size: 12.5px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-line span {
  color: var(--ink-700);
  font-size: 12px;
  font-weight: 600;
  flex: none;
}

.top-bar {
  height: 5px;
  margin: 5px 0 3px;
  border-radius: 999px;
  background: #eef2ef;
  overflow: hidden;
}

.top-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #3fcf8e, #16794c);
  transition: width 0.4s ease;
}

.top-main small {
  color: var(--ink-400);
  font-size: 10.5px;
}

@media (max-width: 1400px) {
  .stat-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1120px) {
  .dash-grid,
  .dash-grid--half {
    grid-template-columns: 1fr;
  }

  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
