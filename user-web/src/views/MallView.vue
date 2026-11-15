<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { mallApi, userApi } from '@/api'
import { usePagedList } from '@/composables/usePagedList'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const keyword = ref('')
const sort = ref('created_desc')
const balance = ref(null)

const { list, total, loading, finished, loadMore, refresh } = usePagedList(({ page, size }) =>
  mallApi.getProducts({
    page,
    size,
    keyword: keyword.value.trim() || undefined,
    sort: sort.value,
  }),
)

onMounted(async () => {
  refresh()
  try {
    const points = await userApi.getPoints()
    balance.value = points.balance
  } catch {
    /* 错误提示由请求拦截器统一处理 */
  }
})
</script>

<template>
  <div class="page">
    <div class="page-body">
      <div class="mall-head">
        <div>
          <h1 class="mall-title">积分商城</h1>
          <p class="muted small">当前可用 {{ balance ?? '—' }} 积分</p>
        </div>
        <router-link to="/me/exchanges" class="my-orders">兑换记录</router-link>
      </div>

      <div class="search-row">
        <el-input v-model="keyword" placeholder="搜索商品" clearable @keyup.enter="refresh" @clear="refresh">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="sort" style="width: 120px" @change="refresh">
          <el-option label="最新上架" value="created_desc" />
          <el-option label="积分从低到高" value="points_asc" />
          <el-option label="积分从高到低" value="points_desc" />
        </el-select>
      </div>

      <el-skeleton v-if="loading && list.length === 0" :rows="4" animated />

      <EmptyState v-else-if="!loading && list.length === 0" text="没有找到相关商品，换个关键词试试" />

      <template v-else>
        <div class="mall-grid">
          <div v-for="item in list" :key="item.productId" class="product-card" @click="router.push(`/mall/${item.productId}`)">
            <div class="cover">
              <img :src="item.coverUrl" :alt="item.name" loading="lazy" />
              <div v-if="item.stock <= 0" class="sold-out">已兑完</div>
            </div>
            <div class="info">
              <div class="name">{{ item.name }}</div>
              <div class="price-row">
                <span class="points">{{ item.pointsPrice }} 积分</span>
                <span class="muted small">库存 {{ item.stock }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="load-more">
          <el-button v-if="!finished" :loading="loading" round @click="loadMore">加载更多</el-button>
          <span v-else class="muted small">共 {{ total }} 件商品</span>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.mall-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin: 4px 4px 14px;
}

.mall-title {
  margin: 0 0 4px;
  font-size: 22px;
  font-weight: 700;
}

.my-orders {
  padding: 7px 12px;
  font-size: 13px;
  color: var(--brand-dark);
  background: var(--brand-soft);
  border-radius: 999px;
}

.search-row {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}
</style>
