import { ref } from 'vue'

/**
 * 用户端列表统一「加载更多」分页（页面清单 UI-02）。
 * fetcher 接收 { page, size }，需返回 { total, list }。
 */
export function usePagedList(fetcher, { size = 10 } = {}) {
  const list = ref([])
  const total = ref(0)
  const loading = ref(false)
  const finished = ref(false)
  const error = ref('')
  let nextPage = 1
  let requestId = 0

  async function load(reset = false) {
    if (loading.value) return
    if (!reset && finished.value) return
    const page = reset ? 1 : nextPage
    if (reset) {
      finished.value = false
      error.value = ''
    }
    const id = ++requestId
    loading.value = true
    try {
      const data = await fetcher({ page, size })
      if (id !== requestId) return
      const rows = data?.list || []
      list.value = reset ? rows : list.value.concat(rows)
      total.value = data?.total ?? list.value.length
      nextPage = page + 1
      finished.value = list.value.length >= total.value || rows.length < size
    } catch (err) {
      if (id === requestId) error.value = err?.message || '加载失败'
    } finally {
      if (id === requestId) loading.value = false
    }
  }

  return {
    list,
    total,
    loading,
    finished,
    error,
    load,
    loadMore: () => load(false),
    refresh: () => load(true),
  }
}
