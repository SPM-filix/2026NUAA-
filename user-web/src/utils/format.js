import dayjs from 'dayjs'

export function formatTime(value) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '—'
}

export function formatDate(value) {
  return value ? dayjs(value).format('YYYY-MM-DD') : '—'
}

export function maskPhone(phone) {
  if (!phone) return '—'
  return String(phone).replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
}

export function signedPoints(change) {
  return `${change > 0 ? '+' : ''}${change}`
}

export const pointsTypeText = {
  INCOME: '收入',
  EXPENSE: '支出',
  REVOKE: '撤销扣回',
  ADJUST: '人工调整',
  REFUND: '退回',
}

export const bizTypeText = {
  DELIVERY: '投递',
  EXCHANGE: '兑换',
  REVOKE: '撤销',
  ADMIN_ADJUST: '管理员调整',
  REFUND: '订单取消退回',
  REWARD: '系统奖励',
}

export const deliveryStatusText = {
  NORMAL: '正常',
  REVOKED: '已撤销',
}

export const orderStatusText = {
  PENDING: '待核销',
  COMPLETED: '已核销',
  CANCELLED: '已取消',
}

export const appealStatusText = {
  PENDING: '待处理',
  ACCEPTED: '已采纳',
  REJECTED: '已驳回',
}

export const announcementTypeText = {
  NOTICE: '系统公告',
  KNOWLEDGE: '环保科普',
}

export const targetTypeText = {
  DELIVERY: '投递记录',
  EXCHANGE: '兑换订单',
}
