export const USER_STATUS = {
  ACTIVE: '正常',
  DISABLED: '已禁用'
}

export const RECYCLER_STATUS = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  DISABLED: '已禁用'
}

export const DELIVERY_STATUS = {
  NORMAL: '正常',
  REVOKED: '已撤销'
}

export const ORDER_STATUS = {
  PENDING: '待核销',
  COMPLETED: '已核销',
  CANCELLED: '已取消'
}

export const PRODUCT_STATUS = {
  ON: '已上架',
  OFF: '已下架'
}

export const ENABLE_STATUS = {
  ENABLED: '启用',
  DISABLED: '停用'
}

export const ANNOUNCEMENT_TYPE = {
  NOTICE: '系统公告',
  KNOWLEDGE: '环保科普'
}

export const ANNOUNCEMENT_STATUS = {
  PUBLISHED: '已发布',
  OFFLINE: '已下线'
}

export const APPEAL_STATUS = {
  PENDING: '待处理',
  ACCEPTED: '已采纳',
  REJECTED: '已驳回'
}

export const APPEAL_TARGET = {
  DELIVERY: '投递流水',
  EXCHANGE: '兑换订单'
}

export const POINTS_TYPE = {
  INCOME: '收入',
  EXPENSE: '支出',
  REVOKE: '撤销扣回',
  ADJUST: '人工调整',
  REFUND: '退回'
}

export const BIZ_TYPE = {
  DELIVERY: '投递',
  EXCHANGE: '兑换',
  REVOKE: '撤销',
  ADMIN_ADJUST: '管理员调整',
  REFUND: '订单取消退回',
  REWARD: '系统奖励'
}

export const ABNORMAL_FLAG = {
  OVER_WEIGHT: '单笔超重',
  HIGH_FREQUENCY: '高频投递'
}

export const STATUS_TAG_TYPE = {
  ACTIVE: 'success',
  DISABLED: 'info',
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  NORMAL: 'success',
  REVOKED: 'info',
  COMPLETED: 'success',
  CANCELLED: 'info',
  ON: 'success',
  OFF: 'info',
  ENABLED: 'success',
  PUBLISHED: 'success',
  OFFLINE: 'info',
  ACCEPTED: 'success'
}

export const POINTS_TAG_TYPE = {
  INCOME: 'success',
  EXPENSE: 'danger',
  REVOKE: 'warning',
  ADJUST: 'primary',
  REFUND: 'success'
}
