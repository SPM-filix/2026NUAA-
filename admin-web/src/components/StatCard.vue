<template>
  <div
    class="stat-card"
    :class="[`tone--${tone}`, { 'is-clickable': clickable }]"
    @click="clickable && $emit('click')"
  >
    <div class="stat-card__top">
      <span class="stat-card__label">{{ label }}</span>
      <span class="stat-card__icon">
        <el-icon><component :is="icon" /></el-icon>
      </span>
    </div>
    <div class="stat-card__value num">
      <el-skeleton v-if="loading" animated :rows="0" style="width: 60%">
        <template #template><el-skeleton-item variant="text" style="height: 28px" /></template>
      </el-skeleton>
      <template v-else>
        {{ value }}<small v-if="unit">{{ unit }}</small>
      </template>
    </div>
    <div class="stat-card__hint">
      <slot name="hint">{{ hint }}</slot>
    </div>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: '—' },
  unit: { type: String, default: '' },
  hint: { type: String, default: '' },
  icon: { type: String, default: 'TrendCharts' },
  tone: { type: String, default: 'green' },
  clickable: { type: Boolean, default: false },
  loading: { type: Boolean, default: false }
})

defineEmits(['click'])
</script>

<style scoped>
.stat-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px 20px 16px;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
  overflow: hidden;
}

.stat-card::after {
  content: '';
  position: absolute;
  inset: auto -30px -46px auto;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.045;
}

.stat-card.is-clickable {
  cursor: pointer;
}

.stat-card.is-clickable:hover {
  transform: translateY(-2px);
  border-color: var(--line-strong);
  box-shadow: 0 10px 28px -18px rgba(16, 36, 25, 0.4);
}

.stat-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.stat-card__label {
  color: var(--ink-500);
  font-size: 12.5px;
  font-weight: 550;
}

.stat-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  font-size: 17px;
  background: var(--tint);
  color: var(--accent);
}

.stat-card__value {
  font-size: 26px;
  font-weight: 660;
  letter-spacing: -0.02em;
  line-height: 1.15;
  color: var(--ink-900);
}

.stat-card__value small {
  margin-left: 4px;
  font-size: 12px;
  font-weight: 500;
  color: var(--ink-400);
  letter-spacing: 0;
}

.stat-card__hint {
  color: var(--ink-400);
  font-size: 11.5px;
  min-height: 17px;
}

.tone--green {
  --accent: #16794c;
  --tint: #e7f4ec;
}

.tone--amber {
  --accent: #a16207;
  --tint: #fbf1dc;
}

.tone--blue {
  --accent: #2f6fa8;
  --tint: #e9f1f9;
}

.tone--plum {
  --accent: #7a4c8c;
  --tint: #f3ecf6;
}

.tone--slate {
  --accent: #4c5f55;
  --tint: #eef2ef;
}
</style>
