const pad = (n) => String(n).padStart(2, '0')

export function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(
    date.getMinutes()
  )}`
}

export function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function formatWeight(value) {
  const n = Number(value)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

export function formatNumber(value) {
  const n = Number(value)
  return Number.isFinite(n) ? n.toLocaleString('zh-CN') : '0'
}

export function formatPercent(value) {
  const n = Number(value)
  return Number.isFinite(n) ? `${Math.round(n * 100)}%` : '0%'
}

export function minutesLeft(deadline, now = Date.now()) {
  if (!deadline) return 0
  return Math.max(0, Math.round((new Date(deadline).getTime() - now) / 60000))
}

export function truncate(value, length = 30) {
  if (!value) return ''
  return value.length > length ? `${value.slice(0, length)}…` : value
}
