<template>
  <div class="page">
    <PageHeader title="积分规则" description="当前生效的垃圾类别与积分系数。规则由管理员配置，回收员仅可查看。" />

    <div class="rules-layout">
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">类别与当前系数</h3>
            <p class="panel-sub">停用类别不会出现在投递登记中，历史流水不受影响</p>
          </div>
          <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        </div>

        <div class="table-wrap">
          <el-table :data="categories" v-loading="loading" empty-text="暂无类别">
            <el-table-column label="类别" min-width="150">
              <template #default="{ row }">
                <div class="cell-title">{{ row.name }}</div>
                <div class="cell-sub mono">{{ row.code }}</div>
              </template>
            </el-table-column>
            <el-table-column label="计量单位" width="100">
              <template #default="{ row }">{{ row.unit || 'kg' }}</template>
            </el-table-column>
            <el-table-column label="每公斤积分" width="130" align="right">
              <template #default="{ row }">
                <span class="rate num">{{ row.pointsPerKg }}</span>
              </template>
            </el-table-column>
            <el-table-column label="生效时间" width="180">
              <template #default="{ row }">{{ formatDateTime(row.ruleEffectiveFrom) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <StatusTag :value="row.status" kind="enable" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <aside class="side">
        <section class="formula-card">
          <p class="formula-card__eyebrow">积分计算公式</p>
          <h3>round(重量 × 每公斤积分)</h3>
          <p class="formula-card__note">计算结果最低为 1 分；重量允许 0.01–100 kg，最多两位小数。</p>
          <div class="example">
            <div><span>废纸 2.40 kg</span><b>2.40 × 10 = 24 分</b></div>
            <div><span>塑料 0.85 kg</span><b>round(12.75) = 13 分</b></div>
            <div><span>金属 0.01 kg</span><b>最低 1 分</b></div>
          </div>
        </section>

        <el-alert
          type="warning"
          :closable="false"
          show-icon
          title="规则调整只影响此后新流水"
          description="历史流水已发放的积分不会重算，这也是需要保留规则版本的原因。"
        />
      </aside>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { listCategories } from '@/api/category'
import { formatDateTime } from '@/utils/format'

const categories = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    categories.value = await listCategories()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.rules-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(300px, 0.65fr);
  gap: 20px;
  align-items: start;
}

.rate {
  color: var(--brand-700);
  font-size: 15px;
  font-weight: 650;
}

.side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.formula-card {
  padding: 22px;
  border-radius: var(--radius-lg);
  background:
    radial-gradient(320px 200px at 100% 0%, rgba(63, 207, 142, 0.18), transparent 70%),
    linear-gradient(150deg, #12603d, #0d3a26);
  color: #fff;
}

.formula-card__eyebrow {
  color: #7cebb5;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.14em;
}

.formula-card h3 {
  margin-top: 10px;
  font-family: var(--font-mono);
  font-size: 17px;
  font-weight: 600;
  line-height: 1.5;
}

.formula-card__note {
  margin-top: 8px;
  color: rgba(226, 240, 232, 0.6);
  font-size: 11.5px;
  line-height: 1.7;
}

.example {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}

.example div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 11.5px;
}

.example span {
  color: rgba(226, 240, 232, 0.6);
}

.example b {
  font-weight: 600;
}

@media (max-width: 1100px) {
  .rules-layout {
    grid-template-columns: 1fr;
  }
}
</style>
