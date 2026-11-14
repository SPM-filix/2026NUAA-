/**
 * 阶段② Mock 服务：按 docs/api.md 的路径、字段与错误码模拟后端。
 * 仅用于前端并行开发；阶段③把 VITE_USE_MOCK 设为 false 即可整体关闭。
 */
import { db, nextId, DEFAULT_RECYCLER_PASSWORD } from './db'

class MockError extends Error {
  constructor(code, message) {
    super(message)
    this.code = code
  }
}

const ok = (data) => ({ code: 0, message: 'ok', data })

/* ------------------------------------------------------------------
 * 时钟：固定演示日期 2026-11-13 14:30 +08:00，保证种子数据与“今天”自洽
 * ------------------------------------------------------------------ */
const MOCK_BASE = new Date('2026-11-13T14:30:00+08:00').getTime()
const BOOT_AT = Date.now()
const now = () => new Date(MOCK_BASE + (Date.now() - BOOT_AT))

function iso(date = now()) {
  const shifted = new Date(date.getTime() + 8 * 3600 * 1000)
  return shifted.toISOString().replace('Z', '+08:00')
}

function dayKey(value = now()) {
  const date = value instanceof Date ? value : new Date(value)
  return new Date(date.getTime() + 8 * 3600 * 1000).toISOString().slice(0, 10)
}

function dayStartOffset(daysAgo) {
  const d = new Date(now().getTime() + 8 * 3600 * 1000)
  d.setUTCDate(d.getUTCDate() - daysAgo)
  return d.toISOString().slice(0, 10)
}

/* ------------------------------------------------------------------
 * 小工具
 * ------------------------------------------------------------------ */
const clone = (v) => JSON.parse(JSON.stringify(v))
const maskPhone = (phone) => (phone ? `${phone.slice(0, 3)}****${phone.slice(7)}` : '')
const round2 = (n) => Math.round(n * 100) / 100
const num = (v) => (v === undefined || v === null || v === '' ? NaN : Number(v))
const trim = (v) => (typeof v === 'string' ? v.trim() : v)

function paginate(list, query = {}, defaultSize = 10) {
  const page = Math.max(1, Number(query.page) || 1)
  const size = Math.min(50, Math.max(1, Number(query.size) || defaultSize))
  const start = (page - 1) * size
  return { total: list.length, page, size, list: list.slice(start, start + size) }
}

function inDateRange(value, from, to) {
  if (!value) return false
  const key = dayKey(value)
  if (from && key < String(from).slice(0, 10)) return false
  if (to && key > String(to).slice(0, 10)) return false
  return true
}

function validatePassword(password) {
  if (typeof password !== 'string' || password.length < 8 || password.length > 20) {
    throw new MockError(2006, '密码需为 8–20 位')
  }
  if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) {
    throw new MockError(2006, '密码需同时包含字母与数字')
  }
}

function randomPassword(length = 10) {
  const letters = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz'
  const digits = '23456789'
  const all = letters + digits
  let out = ''
  for (let i = 0; i < length - 2; i += 1) out += all[Math.floor(Math.random() * all.length)]
  out += letters[Math.floor(Math.random() * letters.length)]
  out += digits[Math.floor(Math.random() * digits.length)]
  return out
}

/* ------------------------------------------------------------------
 * 关系查询
 * ------------------------------------------------------------------ */
const findUser = (id) => db.users.find((u) => u.userId === Number(id))
const findRecycler = (id) => db.recyclers.find((r) => r.recyclerId === Number(id))
const findSite = (id) => db.sites.find((s) => s.siteId === Number(id))
const findCategory = (id) => db.categories.find((c) => c.categoryId === Number(id))
const findProduct = (id) => db.products.find((p) => p.productId === Number(id))
const findAdmin = (id) => db.admins.find((a) => a.adminId === Number(id))

db.recyclers.forEach((r) => {
  if (!r.password) r.password = DEFAULT_RECYCLER_PASSWORD
  if (r.mustChangePassword === undefined) r.mustChangePassword = false
})

/* ------------------------------------------------------------------
 * 视图组装
 * ------------------------------------------------------------------ */
function userAdminView(user) {
  return {
    userId: user.userId,
    userCode: user.userCode,
    nickname: user.nickname,
    phoneMasked: maskPhone(user.phone),
    pointsBalance: user.pointsBalance,
    status: user.status,
    deliveryCount: db.deliveries.filter((d) => d.userId === user.userId).length,
    orderCount: db.orders.filter((o) => o.userId === user.userId).length,
    createdAt: user.createdAt
  }
}

function userLookupView(user) {
  return {
    userId: user.userId,
    userCode: user.userCode,
    nickname: user.nickname,
    phoneMasked: maskPhone(user.phone),
    avatarUrl: user.avatarUrl,
    pointsBalance: user.pointsBalance,
    status: user.status
  }
}

function recyclerView(recycler) {
  const site = findSite(recycler.siteId)
  return {
    ...recycler,
    siteName: site?.name ?? recycler.siteName,
    password: undefined,
    mustChangePassword: undefined
  }
}

function abnormalFlags(record) {
  const flags = []
  if (Number(record.weight) > 50) flags.push('OVER_WEIGHT')
  const created = new Date(record.createdAt).getTime()
  const sameUser = db.deliveries.filter(
    (d) =>
      d.userId === record.userId &&
      d.status === 'NORMAL' &&
      Math.abs(new Date(d.createdAt).getTime() - created) <= 60 * 60 * 1000
  )
  if (sameUser.length >= 5) flags.push('HIGH_FREQUENCY')
  return flags
}

function deliveryView(record) {
  const user = findUser(record.userId)
  const recycler = findRecycler(record.recyclerId)
  const category = findCategory(record.categoryId)
  const site = record.siteId ? findSite(record.siteId) : recycler ? findSite(recycler.siteId) : null
  return {
    ...record,
    userNickname: user?.nickname ?? record.userNickname,
    userPhoneMasked: maskPhone(user?.phone),
    recyclerName: recycler?.name ?? record.recyclerName,
    siteName: site?.name ?? record.siteName,
    categoryName: category?.name ?? record.categoryName,
    abnormalFlags: abnormalFlags(record)
  }
}

function orderView(order) {
  const user = findUser(order.userId)
  return {
    ...order,
    userNickname: user?.nickname ?? order.userNickname
  }
}

function productView(product) {
  return {
    ...product,
    salesCount: db.orders
      .filter((o) => o.productId === product.productId && o.status === 'COMPLETED')
      .reduce((sum, o) => sum + o.quantity, 0)
  }
}

/* ------------------------------------------------------------------
 * 积分变动
 * ------------------------------------------------------------------ */
function applyPoints(user, change, { type, bizType, bizNo = null, remark = null, operatorType = 'SYSTEM' }) {
  if (change === 0) throw new MockError(1001, '变动值不能为 0')
  if (user.pointsBalance + change < 0) {
    throw new MockError(3011, '积分余额不足，操作会导致余额为负')
  }
  user.pointsBalance += change
  const log = {
    logId: nextId(),
    userId: user.userId,
    type,
    change,
    balanceAfter: user.pointsBalance,
    bizType,
    bizNo,
    remark,
    operatorType,
    createdAt: iso(now())
  }
  db.pointsLogs.unshift(log)
  return log
}

/* ------------------------------------------------------------------
 * 路由
 * ------------------------------------------------------------------ */
const routes = []

function route(method, path, handler, options = {}) {
  const keys = []
  const pattern = new RegExp(
    `^${path.replace(/:([A-Za-z]+)/g, (_, key) => {
      keys.push(key)
      return '([^/]+)'
    })}$`
  )
  routes.push({ method, pattern, keys, handler, ...options })
}

function requireRole(auth, role) {
  if (!auth) throw new MockError(1002, '登录已过期，请重新登录')
  if (auth.role !== role) throw new MockError(1003, '当前角色无权访问该接口')
}

function requireAuth(auth) {
  if (!auth) throw new MockError(1002, '登录已过期，请重新登录')
}

/* ---------------------------- 认证 ---------------------------- */
route('POST', '/auth/admins/login', ({ body }) => {
  const username = trim(body?.username)
  const password = body?.password
  const admin = db.admins.find((a) => a.username === username)
  if (!admin || admin.password !== password) throw new MockError(2001, '账号或密码错误')
  return ok({
    token: `mock.ADMIN.${admin.adminId}.${Date.now()}`,
    tokenType: 'Bearer',
    expiresIn: 7200,
    mustChangePassword: Boolean(admin.mustChangePassword),
    admin: { adminId: admin.adminId, username: admin.username }
  })
}, { public: true })

route('POST', '/auth/recyclers/login', ({ body }) => {
  const phone = trim(body?.phone)
  const password = body?.password
  const recycler = db.recyclers.find((r) => r.phone === phone)
  if (!recycler || recycler.password !== password) throw new MockError(2001, '账号或密码错误')
  if (recycler.status === 'PENDING') {
    throw new MockError(2004, '回收员申请正在审核中，请等待管理员通过')
  }
  if (recycler.status === 'REJECTED') {
    throw new MockError(2004, `回收员申请已被驳回：${recycler.rejectReason || '请重新提交申请'}`)
  }
  if (recycler.status === 'DISABLED') throw new MockError(2002, '账号已被禁用，请联系管理员')

  return ok({
    token: `mock.RECYCLER.${recycler.recyclerId}.${Date.now()}`,
    tokenType: 'Bearer',
    expiresIn: 7200,
    mustChangePassword: Boolean(recycler.mustChangePassword),
    recycler: {
      recyclerId: recycler.recyclerId,
      name: recycler.name,
      siteId: recycler.siteId,
      siteName: findSite(recycler.siteId)?.name ?? null,
      status: recycler.status
    }
  })
}, { public: true })

route('POST', '/auth/logout', ({ auth }) => {
  requireAuth(auth)
  return ok(null)
})

route('PUT', '/auth/password', ({ auth, body }) => {
  requireAuth(auth)
  const oldPassword = body?.oldPassword
  const newPassword = body?.newPassword
  const account =
    auth.role === 'ADMIN' ? findAdmin(auth.id) : auth.role === 'RECYCLER' ? findRecycler(auth.id) : null
  if (!account) throw new MockError(1005, '账号不存在')
  if (account.password !== oldPassword) throw new MockError(2005, '原密码错误')
  validatePassword(newPassword)
  if (oldPassword === newPassword) throw new MockError(2006, '新密码不能与原密码相同')
  account.password = newPassword
  account.mustChangePassword = false
  return ok(null)
})

route('GET', '/admins/me', ({ auth }) => {
  requireRole(auth, 'ADMIN')
  const admin = findAdmin(auth.id)
  if (!admin) throw new MockError(1005, '管理员不存在')
  return ok({ adminId: admin.adminId, username: admin.username, createdAt: admin.createdAt })
})

route('GET', '/recyclers/me', ({ auth }) => {
  requireRole(auth, 'RECYCLER')
  const recycler = findRecycler(auth.id)
  if (!recycler) throw new MockError(1005, '回收员不存在')
  return ok({
    recyclerId: recycler.recyclerId,
    name: recycler.name,
    phone: recycler.phone,
    employeeNo: recycler.employeeNo,
    siteId: recycler.siteId,
    siteName: findSite(recycler.siteId)?.name ?? null,
    status: recycler.status,
    createdAt: recycler.createdAt
  })
})

route('GET', '/health', () => ok({ status: 'UP', time: iso(now()), version: 'mock-0.1.0' }), { public: true })

/* ---------------------------- 公共只读 ---------------------------- */
route('GET', '/categories', ({ query }) => {
  let list = clone(db.categories)
  if (String(query.enabledOnly) === 'true') list = list.filter((c) => c.status === 'ENABLED')
  return ok(list.sort((a, b) => a.sort - b.sort))
})

route('GET', '/sites', ({ query }) => {
  let list = clone(db.sites)
  if (String(query.enabledOnly) === 'true') list = list.filter((s) => s.status === 'ENABLED')
  return ok(list)
})

route('GET', '/products', ({ query }) => {
  let list = db.products.map(productView)
  const keyword = trim(query.keyword)
  if (keyword) list = list.filter((p) => p.name.includes(keyword))
  if (String(query.onlyAvailable) === 'true') {
    list = list.filter((p) => p.stock > 0 && p.status === 'ON')
  }
  const sort = query.sort
  if (sort === 'points_asc') list.sort((a, b) => a.pointsPrice - b.pointsPrice)
  else if (sort === 'points_desc') list.sort((a, b) => b.pointsPrice - a.pointsPrice)
  else list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  return ok(paginate(list, query, 10))
})

route('GET', '/products/:productId', ({ params }) => {
  const product = findProduct(params.productId)
  if (!product) throw new MockError(1005, '商品不存在')
  return ok(productView(product))
})

route('GET', '/announcements', ({ query }) => {
  let list = db.announcements
    .filter((a) => a.status === 'PUBLISHED')
    .map((a) => ({
      announcementId: a.announcementId,
      title: a.title,
      type: a.type,
      summary: a.content.slice(0, 100),
      coverUrl: a.coverUrl,
      publishedAt: a.publishedAt
    }))
  if (query.type) list = list.filter((a) => a.type === query.type)
  return ok(paginate(list, query, 10))
})

route('GET', '/announcements/:announcementId', ({ params }) => {
  const item = db.announcements.find((a) => a.announcementId === Number(params.announcementId))
  if (!item) throw new MockError(1005, '公告不存在')
  return ok(item)
})

/* ---------------------------- 回收员 ---------------------------- */
route('GET', '/users/lookup', ({ auth, query }) => {
  requireRole(auth, 'RECYCLER')
  let user = null
  if (query.keyword) {
    const keyword = trim(query.keyword)
    user = db.users.find((u) => u.phone === keyword || u.userCode === keyword)
  } else if (query.qrContent) {
    const match = /^NUAA-RECYCLE:U:(.+)$/.exec(trim(query.qrContent))
    if (!match) throw new MockError(1001, '二维码内容无效或已过期')
    const payload = match[1]
    user = db.users.find((u) => u.userCode === payload || `mock-user-${u.userId}` === payload)
    if (!user) {
      // 兼容直接粘贴 6 位编号的演示场景
      user = db.users.find((u) => u.userCode === payload.slice(-6))
    }
  } else {
    throw new MockError(1001, '请输入手机号、用户编号或扫描二维码')
  }
  if (!user) throw new MockError(1005, '未找到该用户')
  if (user.status === 'DISABLED') throw new MockError(2002, '该用户已被禁用，无法发放积分')
  return ok(userLookupView(user))
})

route('POST', '/deliveries', ({ auth, body }) => {
  requireRole(auth, 'RECYCLER')
  const user = findUser(body?.userId)
  const category = findCategory(body?.categoryId)
  const weight = num(body?.weight)

  if (!user) throw new MockError(1005, '用户不存在')
  if (!category) throw new MockError(1005, '垃圾类别不存在')
  if (category.status !== 'ENABLED') throw new MockError(3009, '该类别已停用，请选择其他类别')
  if (!Number.isFinite(weight) || weight < 0.01 || weight > 100) {
    throw new MockError(3010, '重量需在 0.01–100 kg 之间')
  }
  if (Math.round(weight * 100) !== weight * 100) {
    throw new MockError(3010, '重量最多保留两位小数')
  }
  if (user.status !== 'ACTIVE') throw new MockError(2002, '该用户已被禁用，无法发放积分')

  const points = Math.max(1, Math.round(weight * category.pointsPerKg))
  const recycler = findRecycler(auth.id)
  const createdAt = iso(now())
  const recordId = nextId()
  const datePart = dayKey(now()).replace(/-/g, '')
  const sameDayCount = db.deliveries.filter((d) => d.recordNo.startsWith(`D${datePart}`)).length + 1
  const recordNo = `D${datePart}${String(sameDayCount).padStart(3, '0')}`

  const record = {
    recordId,
    recordNo,
    userId: user.userId,
    userNickname: user.nickname,
    userPhoneMasked: maskPhone(user.phone),
    recyclerId: recycler.recyclerId,
    recyclerName: recycler.name,
    siteId: recycler.siteId,
    siteName: findSite(recycler.siteId)?.name ?? null,
    categoryId: category.categoryId,
    categoryName: category.name,
    weight: round2(weight),
    points,
    status: 'NORMAL',
    remark: trim(body?.remark) || null,
    revokeReason: null,
    revokedBy: null,
    revokedByType: null,
    revokedAt: null,
    abnormalFlags: [],
    createdAt
  }
  db.deliveries.unshift(record)
  applyPoints(user, points, {
    type: 'INCOME',
    bizType: 'DELIVERY',
    bizNo: recordNo,
    remark: `${category.name} ${weight.toFixed(2)} kg`,
    operatorType: 'RECYCLER'
  })

  return ok({
    recordId,
    recordNo,
    points,
    weight: round2(weight),
    categoryName: category.name,
    balanceAfter: user.pointsBalance,
    createdAt,
    revocableBefore: iso(new Date(now().getTime() + 5 * 60 * 1000))
  })
})

route('GET', '/recyclers/me/deliveries', ({ auth, query }) => {
  requireRole(auth, 'RECYCLER')
  let list = db.deliveries
    .filter((d) => d.recyclerId === auth.id)
    .filter((d) => !query.categoryId || d.categoryId === Number(query.categoryId))
    .filter((d) => !query.status || d.status === query.status)
    .filter((d) => !query.from && !query.to ? true : inDateRange(d.createdAt, query.from, query.to))
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .map((d) => {
      const view = deliveryView(d)
      return {
        recordId: view.recordId,
        recordNo: view.recordNo,
        userNickname: view.userNickname,
        userPhoneMasked: view.userPhoneMasked,
        categoryId: view.categoryId,
        categoryName: view.categoryName,
        weight: view.weight,
        points: view.points,
        status: view.status,
        revokeReason: view.revokeReason,
        revokedAt: view.revokedAt,
        createdAt: view.createdAt,
        revocable:
          view.status === 'NORMAL' && now().getTime() - new Date(view.createdAt).getTime() <= 5 * 60 * 1000
      }
    })
  const { page, size, total, list: pageList } = paginate(list, query, 10)
  return ok({ total, page, size, list: pageList })
})

route('POST', '/deliveries/:recordId/revoke', ({ auth, params, body }) => {
  requireRole(auth, 'RECYCLER')
  const record = db.deliveries.find((d) => d.recordId === Number(params.recordId))
  if (!record || record.recyclerId !== auth.id) throw new MockError(3006, '流水不存在或不属于本人')
  if (record.status === 'REVOKED') throw new MockError(3008, '该流水已撤销')
  if (now().getTime() - new Date(record.createdAt).getTime() > 5 * 60 * 1000) {
    throw new MockError(3007, '已超过 5 分钟撤销窗口，需管理员授权')
  }
  const reason = trim(body?.reason)
  if (!reason || reason.length < 10 || reason.length > 100) {
    throw new MockError(1001, '撤销原因需为 10–100 字')
  }
  const user = findUser(record.userId)
  if (user.pointsBalance < record.points) {
    throw new MockError(3001, '用户余额不足以扣回本次积分')
  }
  const recycler = findRecycler(auth.id)
  record.status = 'REVOKED'
  record.revokeReason = reason
  record.revokedBy = recycler?.name ?? null
  record.revokedByType = 'RECYCLER'
  record.revokedAt = iso(now())
  applyPoints(user, -record.points, {
    type: 'REVOKE',
    bizType: 'REVOKE',
    bizNo: record.recordNo,
    remark: reason,
    operatorType: 'RECYCLER'
  })
  return ok({
    recordId: record.recordId,
    status: 'REVOKED',
    refundPoints: record.points,
    balanceAfter: user.pointsBalance,
    revokedAt: record.revokedAt
  })
})

route('GET', '/deliveries/:recordId', ({ auth, params }) => {
  requireAuth(auth)
  const record = db.deliveries.find((d) => d.recordId === Number(params.recordId))
  if (!record) throw new MockError(1005, '流水不存在')
  if (auth.role === 'RECYCLER' && record.recyclerId !== auth.id) {
    throw new MockError(1003, '无权查看他人的流水')
  }
  return ok(deliveryView(record))
})

route('GET', '/exchanges/lookup', ({ auth, query }) => {
  requireRole(auth, 'RECYCLER')
  const code = String(query.redeemCode || '').trim().toUpperCase()
  const order = db.orders.find((o) => o.redeemCode === code)
  if (!order) throw new MockError(3005, '核销码无效或不存在')
  if (order.status !== 'PENDING') throw new MockError(3005, '该订单已核销或已取消')
  if (new Date(order.redeemCodeExpireAt) < now()) throw new MockError(3005, '核销码已过期')
  const user = findUser(order.userId)
  return ok({
    orderId: order.orderId,
    orderNo: order.orderNo,
    userNickname: order.userNickname,
    userPhoneMasked: maskPhone(user?.phone),
    productName: order.productName,
    productCoverUrl: order.productCoverUrl,
    quantity: order.quantity,
    pointsCost: order.pointsCost,
    status: order.status,
    createdAt: order.createdAt,
    redeemCodeExpireAt: order.redeemCodeExpireAt
  })
})

route('POST', '/exchanges/redeem', ({ auth, body }) => {
  requireRole(auth, 'RECYCLER')
  const code = String(body?.redeemCode || '').trim().toUpperCase()
  const order = db.orders.find((o) => o.redeemCode === code)
  if (!order) throw new MockError(3005, '核销码无效或不可核销')
  if (order.status !== 'PENDING') throw new MockError(3005, '该订单已核销或已取消')
  if (new Date(order.redeemCodeExpireAt) < now()) throw new MockError(3005, '核销码已过期')

  const recycler = findRecycler(auth.id)
  order.status = 'COMPLETED'
  order.verifiedAt = iso(now())
  order.verifierId = recycler.recyclerId
  order.verifierName = recycler.name
  order.siteName = findSite(recycler.siteId)?.name ?? null

  return ok({
    orderId: order.orderId,
    orderNo: order.orderNo,
    productName: order.productName,
    quantity: order.quantity,
    userNickname: order.userNickname,
    status: 'COMPLETED',
    verifiedAt: order.verifiedAt,
    verifierName: order.verifierName
  })
})

route('GET', '/recyclers/me/redemptions', ({ auth, query }) => {
  requireRole(auth, 'RECYCLER')
  const recycler = findRecycler(auth.id)
  const list = db.orders
    .filter((o) => o.status === 'COMPLETED' && (o.verifierId === auth.id || o.verifierName === recycler.name))
    .filter((o) => !query.from && !query.to ? true : inDateRange(o.verifiedAt, query.from, query.to))
    .sort((a, b) => new Date(b.verifiedAt) - new Date(a.verifiedAt))
    .map((o) => ({
      orderId: o.orderId,
      orderNo: o.orderNo,
      userNickname: o.userNickname,
      productName: o.productName,
      quantity: o.quantity,
      pointsCost: o.pointsCost,
      verifiedAt: o.verifiedAt
    }))
  return ok(paginate(list, query, 10))
})

route('GET', '/recyclers/me/workbench', ({ auth }) => {
  requireRole(auth, 'RECYCLER')
  const today = dayKey(now())
  const recycler = findRecycler(auth.id)
  const mine = db.deliveries.filter((d) => d.recyclerId === auth.id && d.status === 'NORMAL')
  const todayList = mine.filter((d) => dayKey(d.createdAt) === today)
  const todayRedeems = db.orders.filter(
    (o) =>
      o.status === 'COMPLETED' &&
      (o.verifierId === auth.id || o.verifierName === recycler.name) &&
      dayKey(o.verifiedAt) === today
  )
  const weekTrend = Array.from({ length: 7 }, (_, index) => {
    const date = dayStartOffset(6 - index)
    return {
      date,
      weight: round2(
        mine.filter((d) => dayKey(d.createdAt) === date).reduce((sum, d) => sum + Number(d.weight), 0)
      )
    }
  })
  const monthPrefix = today.slice(0, 7)
  const colleagues = db.recyclers.filter((r) => r.siteId === recycler.siteId && r.status === 'APPROVED')
  const ranking = colleagues
    .map((r) => ({
      id: r.recyclerId,
      points: db.deliveries
        .filter(
          (d) =>
            d.recyclerId === r.recyclerId &&
            d.status === 'NORMAL' &&
            dayKey(d.createdAt).startsWith(monthPrefix)
        )
        .reduce((sum, d) => sum + d.points, 0)
    }))
    .sort((a, b) => b.points - a.points)
  const rank = ranking.findIndex((r) => r.id === auth.id)
  return ok({
    todayWeight: round2(todayList.reduce((sum, d) => sum + Number(d.weight), 0)),
    todayPoints: todayList.reduce((sum, d) => sum + d.points, 0),
    todayDeliveryCount: todayList.length,
    todayRedeemCount: todayRedeems.length,
    siteName: findSite(recycler.siteId)?.name ?? null,
    weekTrend,
    monthRank: rank >= 0 ? rank + 1 : null
  })
})

/* ---------------------------- 管理员：统计 ---------------------------- */
route('GET', '/admin/stats/overview', ({ auth }) => {
  requireRole(auth, 'ADMIN')
  const normal = db.deliveries.filter((d) => d.status === 'NORMAL')
  const completed = db.orders.filter((o) => o.status === 'COMPLETED')
  return ok({
    totalWeight: round2(normal.reduce((sum, d) => sum + Number(d.weight), 0)),
    totalPointsIssued: normal.reduce((sum, d) => sum + d.points, 0),
    totalPointsRedeemed: completed.reduce((sum, o) => sum + o.pointsCost, 0),
    userCount: db.users.length,
    recyclerCount: db.recyclers.filter((r) => r.status === 'APPROVED').length,
    completedOrderCount: completed.length,
    pendingRecyclerCount: db.recyclers.filter((r) => r.status === 'PENDING').length,
    pendingAppealCount: db.appeals.filter((a) => a.status === 'PENDING').length,
    expiredPendingOrderCount: db.orders.filter(
      (o) => o.status === 'PENDING' && new Date(o.redeemCodeExpireAt) < now()
    ).length
  })
})

route('GET', '/admin/stats/categories', ({ auth, query }) => {
  requireRole(auth, 'ADMIN')
  const records = db.deliveries
    .filter((d) => d.status === 'NORMAL')
    .filter((d) => (!query.from && !query.to ? true : inDateRange(d.createdAt, query.from, query.to)))
  const totalWeight = records.reduce((sum, d) => sum + Number(d.weight), 0)
  const grouped = new Map()
  records.forEach((d) => {
    const item = grouped.get(d.categoryId) || {
      categoryId: d.categoryId,
      categoryName: d.categoryName,
      weight: 0,
      points: 0,
      recordCount: 0
    }
    item.weight += Number(d.weight)
    item.points += d.points
    item.recordCount += 1
    grouped.set(d.categoryId, item)
  })
  const list = [...grouped.values()]
    .map((item) => ({
      ...item,
      weight: round2(item.weight),
      weightRatio: totalWeight ? round2(item.weight / totalWeight) : 0
    }))
    .sort((a, b) => b.weight - a.weight)
  return ok(list)
})

route('GET', '/admin/stats/trend', ({ auth, query }) => {
  requireRole(auth, 'ADMIN')
  const days = [7, 30].includes(Number(query.days)) ? Number(query.days) : 7
  const list = Array.from({ length: days }, (_, index) => {
    const date = dayStartOffset(days - 1 - index)
    const deliveries = db.deliveries.filter((d) => d.status === 'NORMAL' && dayKey(d.createdAt) === date)
    const orders = db.orders.filter((o) => dayKey(o.createdAt) === date)
    return {
      date,
      weight: round2(deliveries.reduce((sum, d) => sum + Number(d.weight), 0)),
      points: deliveries.reduce((sum, d) => sum + d.points, 0),
      orderCount: orders.length,
      deliveryCount: deliveries.length
    }
  })
  return ok(list)
})

route('GET', '/admin/stats/top', ({ auth, query }) => {
  requireRole(auth, 'ADMIN')
  const type = query.type === 'USER' ? 'USER' : 'PRODUCT'
  const limit = Math.min(20, Math.max(1, Number(query.limit) || 5))
  let list = []
  if (type === 'PRODUCT') {
    const grouped = new Map()
    db.orders
      .filter((o) => o.status === 'COMPLETED')
      .forEach((o) => {
        const item = grouped.get(o.productId) || { id: o.productId, name: o.productName, value: 0, extra: 0 }
        item.value += o.quantity
        item.extra += o.pointsCost
        grouped.set(o.productId, item)
      })
    list = [...grouped.values()]
  } else {
    const grouped = new Map()
    db.deliveries
      .filter((d) => d.status === 'NORMAL')
      .forEach((d) => {
        const item = grouped.get(d.userId) || {
          id: d.userId,
          name: `${String(d.userNickname || '').slice(0, 1)}**`,
          value: 0,
          extra: 0
        }
        item.value += Number(d.weight)
        item.extra += d.points
        grouped.set(d.userId, item)
      })
    list = [...grouped.values()].map((item) => ({ ...item, value: round2(item.value) }))
  }
  return ok(list.sort((a, b) => b.value - a.value).slice(0, limit))
})

/* ---------------------------- 管理员：流水 ---------------------------- */
route('GET', '/admin/deliveries', ({ auth, query }) => {
  requireRole(auth, 'ADMIN')
  let list = db.deliveries.map(deliveryView)
  if (query.recyclerId) list = list.filter((d) => d.recyclerId === Number(query.recyclerId))
  if (query.categoryId) list = list.filter((d) => d.categoryId === Number(query.categoryId))
  if (query.status) list = list.filter((d) => d.status === query.status)
  if (query.from || query.to) list = list.filter((d) => inDateRange(d.createdAt, query.from, query.to))
  if (String(query.abnormalOnly) === 'true') list = list.filter((d) => d.abnormalFlags.length > 0)
  list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))

  const { page, size, total, list: pageList } = paginate(list, query, 20)
  return ok({
    total,
    page,
    size,
    list: pageList,
    summary: {
      totalCount: list.length,
      totalWeight: round2(list.filter((d) => d.status === 'NORMAL').reduce((sum, d) => sum + Number(d.weight), 0)),
      totalPoints: list.filter((d) => d.status === 'NORMAL').reduce((sum, d) => sum + d.points, 0),
      revokedCount: list.filter((d) => d.status === 'REVOKED').length
    }
  })
})

route('POST', '/admin/deliveries/:recordId/revoke', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const record = db.deliveries.find((d) => d.recordId === Number(params.recordId))
  if (!record) throw new MockError(3006, '流水不存在')
  if (record.status === 'REVOKED') throw new MockError(3008, '该流水已撤销')
  const reason = trim(body?.reason)
  if (!reason || reason.length < 10 || reason.length > 200) {
    throw new MockError(1001, '授权撤销原因需为 10–200 字')
  }
  const user = findUser(record.userId)
  const admin = findAdmin(auth.id)
  let deficit = 0
  let deduction = record.points
  if (user.pointsBalance < record.points) {
    deduction = user.pointsBalance
    deficit = record.points - user.pointsBalance
  }
  record.status = 'REVOKED'
  record.revokeReason = reason
  record.revokedBy = admin?.username ?? 'admin'
  record.revokedByType = 'ADMIN'
  record.revokedAt = iso(now())
  user.pointsBalance -= deduction
  if (deduction > 0) {
    db.pointsLogs.unshift({
      logId: nextId(),
      userId: user.userId,
      type: 'REVOKE',
      change: -deduction,
      balanceAfter: user.pointsBalance,
      bizType: 'REVOKE',
      bizNo: record.recordNo,
      remark: reason,
      operatorType: 'ADMIN',
      createdAt: iso(now())
    })
  }
  return ok({
    recordId: record.recordId,
    status: 'REVOKED',
    refundPoints: record.points,
    balanceAfter: user.pointsBalance,
    revokedAt: record.revokedAt,
    revokedBy: record.revokedBy,
    deficit
  })
})

/* ---------------------------- 管理员：订单 ---------------------------- */
route('GET', '/admin/exchanges', ({ auth, query }) => {
  requireRole(auth, 'ADMIN')
  let list = db.orders.map(orderView)
  if (query.status) list = list.filter((o) => o.status === query.status)
  if (query.userId) list = list.filter((o) => o.userId === Number(query.userId))
  if (query.orderNo) list = list.filter((o) => o.orderNo.includes(trim(query.orderNo)))
  list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  return ok(paginate(list, query, 20))
})

route('GET', '/exchanges/:orderId', ({ auth, params }) => {
  requireAuth(auth)
  const order = db.orders.find((o) => o.orderId === Number(params.orderId))
  if (!order) throw new MockError(1005, '订单不存在')
  if (auth.role === 'USER' && order.userId !== auth.id) throw new MockError(1003, '无权查看该订单')
  return ok(orderView(order))
})

route('POST', '/admin/exchanges/:orderId/cancel', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const order = db.orders.find((o) => o.orderId === Number(params.orderId))
  if (!order) throw new MockError(1005, '订单不存在')
  if (order.status !== 'PENDING') throw new MockError(3012, '仅待核销订单可以取消')
  const reason = trim(body?.reason)
  if (!reason || reason.length < 10 || reason.length > 200) {
    throw new MockError(1001, '取消原因需为 10–200 字')
  }
  const user = findUser(order.userId)
  const product = findProduct(order.productId)
  order.status = 'CANCELLED'
  order.cancelReason = reason
  if (product) product.stock += order.quantity
  const log = applyPoints(user, order.pointsCost, {
    type: 'REFUND',
    bizType: 'REFUND',
    bizNo: order.orderNo,
    remark: reason,
    operatorType: 'ADMIN'
  })
  return ok({
    orderId: order.orderId,
    status: 'CANCELLED',
    refundPoints: order.pointsCost,
    restoredStock: order.quantity,
    balanceAfter: log.balanceAfter
  })
})

/* ---------------------------- 管理员：商品 ---------------------------- */
route('POST', '/admin/products', ({ auth, body }) => {
  requireRole(auth, 'ADMIN')
  const name = trim(body?.name)
  const pointsPrice = num(body?.pointsPrice)
  const stock = num(body?.stock)
  if (!name || name.length > 50) throw new MockError(1001, '商品名称需为 1–50 字')
  if (!Number.isInteger(pointsPrice) || pointsPrice < 1 || pointsPrice > 100000) {
    throw new MockError(1001, '所需积分需为 1–100000 的整数')
  }
  if (!Number.isInteger(stock) || stock < 0 || stock > 99999) {
    throw new MockError(1001, '库存需为 0–99999 的整数')
  }
  const createdAt = iso(now())
  const product = {
    productId: nextId(),
    name,
    coverUrl: body?.coverUrl || null,
    pointsPrice,
    stock,
    status: 'ON',
    description: trim(body?.description) || null,
    spec: trim(body?.spec) || null,
    exchangeNotice: trim(body?.exchangeNotice) || null,
    salesCount: 0,
    createdAt,
    updatedAt: createdAt
  }
  db.products.unshift(product)
  return ok({ productId: product.productId })
})

route('PUT', '/admin/products/:productId', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const product = findProduct(params.productId)
  if (!product) throw new MockError(1005, '商品不存在')
  const fields = ['name', 'coverUrl', 'pointsPrice', 'description', 'spec', 'exchangeNotice']
  fields.forEach((field) => {
    if (body?.[field] !== undefined && body[field] !== null) {
      product[field] = typeof body[field] === 'string' ? body[field].trim() : body[field]
    }
  })
  if (product.name && product.name.length > 50) throw new MockError(1001, '商品名称需为 1–50 字')
  product.updatedAt = iso(now())
  return ok(productView(product))
})

route('PATCH', '/admin/products/:productId/status', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const product = findProduct(params.productId)
  if (!product) throw new MockError(1005, '商品不存在')
  if (!['ON', 'OFF'].includes(body?.status)) throw new MockError(1001, '状态取值应为 ON / OFF')
  product.status = body.status
  product.updatedAt = iso(now())
  return ok({ productId: product.productId, status: product.status })
})

route('PATCH', '/admin/products/:productId/stock', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const product = findProduct(params.productId)
  if (!product) throw new MockError(1005, '商品不存在')
  const mode = body?.mode
  const value = num(body?.value)
  const reason = trim(body?.reason)
  if (!['SET', 'INCR'].includes(mode)) throw new MockError(1001, '调整模式应为 SET / INCR')
  if (!Number.isInteger(value)) throw new MockError(1001, '调整数量必须为整数')
  if (!reason) throw new MockError(1001, '请填写库存调整原因')
  const stockBefore = product.stock
  const stock = mode === 'SET' ? value : stockBefore + value
  if (stock < 0) throw new MockError(3011, '调整后库存不能为负')
  if (stock > 99999) throw new MockError(1001, '库存不能超过 99999')
  product.stock = stock
  product.updatedAt = iso(now())
  return ok({ productId: product.productId, stockBefore, stock, adjustId: nextId() })
})

route('DELETE', '/admin/products/:productId', ({ auth, params }) => {
  requireRole(auth, 'ADMIN')
  const product = findProduct(params.productId)
  if (!product) throw new MockError(1005, '商品不存在')
  if (db.orders.some((o) => o.productId === product.productId)) {
    throw new MockError(1006, '该商品存在关联订单，不能删除，请改为下架')
  }
  db.products = db.products.filter((p) => p.productId !== product.productId)
  return ok(null)
})

/* ---------------------------- 管理员：类别与规则 ---------------------------- */
route('POST', '/admin/categories', ({ auth, body }) => {
  requireRole(auth, 'ADMIN')
  const name = trim(body?.name)
  const code = trim(body?.code)
  const pointsPerKg = num(body?.pointsPerKg)
  if (!name || name.length > 20) throw new MockError(1001, '类别名称需为 1–20 字')
  if (!/^[A-Z][A-Z_]*$/.test(code || '')) throw new MockError(1001, '编码需为大写字母与下划线')
  if (db.categories.some((c) => c.code === code)) throw new MockError(1001, '类别编码已存在')
  if (!Number.isInteger(pointsPerKg) || pointsPerKg < 1) throw new MockError(1001, '积分系数需为不小于 1 的整数')

  const createdAt = iso(now())
  const category = {
    categoryId: nextId(),
    name,
    code,
    unit: trim(body?.unit) || 'kg',
    iconUrl: body?.iconUrl || null,
    sort: Number(body?.sort) || 0,
    status: 'ENABLED',
    pointsPerKg,
    ruleEffectiveFrom: createdAt
  }
  db.categories.push(category)
  db.rules.push({
    ruleId: nextId(),
    categoryId: category.categoryId,
    pointsPerKg,
    effectiveFrom: createdAt,
    createdBy: findAdmin(auth.id)?.username ?? 'admin',
    createdAt
  })
  return ok({ categoryId: category.categoryId })
})

route('PUT', '/admin/categories/:categoryId', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const category = findCategory(params.categoryId)
  if (!category) throw new MockError(1005, '类别不存在')
  ;['name', 'unit', 'iconUrl', 'sort'].forEach((field) => {
    if (body?.[field] !== undefined && body[field] !== null) {
      category[field] = field === 'sort' ? Number(body[field]) : trim(body[field])
    }
  })
  return ok(category)
})

route('DELETE', '/admin/categories/:categoryId', ({ auth, params }) => {
  requireRole(auth, 'ADMIN')
  const category = findCategory(params.categoryId)
  if (!category) throw new MockError(1005, '类别不存在')
  if (db.deliveries.some((d) => d.categoryId === category.categoryId)) {
    throw new MockError(1006, '该类别已有投递流水，不能删除，请改为停用')
  }
  db.categories = db.categories.filter((c) => c.categoryId !== category.categoryId)
  return ok(null)
})

route('PATCH', '/admin/categories/:categoryId/status', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const category = findCategory(params.categoryId)
  if (!category) throw new MockError(1005, '类别不存在')
  if (!['ENABLED', 'DISABLED'].includes(body?.status)) {
    throw new MockError(1001, '状态取值应为 ENABLED / DISABLED')
  }
  category.status = body.status
  return ok({ categoryId: category.categoryId, status: category.status })
})

route('PUT', '/admin/categories/:categoryId/rule', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const category = findCategory(params.categoryId)
  if (!category) throw new MockError(1005, '类别不存在')
  const pointsPerKg = num(body?.pointsPerKg)
  if (!Number.isInteger(pointsPerKg) || pointsPerKg < 1 || pointsPerKg > 1000) {
    throw new MockError(1001, '每公斤积分需为 1–1000 的整数')
  }
  const effectiveFrom = body?.effectiveFrom ? iso(new Date(body.effectiveFrom)) : iso(now())
  if (new Date(effectiveFrom) < new Date(category.ruleEffectiveFrom)) {
    throw new MockError(1001, '生效时间不能早于当前规则的生效时间')
  }
  const rule = {
    ruleId: nextId(),
    categoryId: category.categoryId,
    pointsPerKg,
    effectiveFrom,
    createdBy: findAdmin(auth.id)?.username ?? 'admin',
    createdAt: iso(now())
  }
  db.rules.push(rule)
  if (new Date(effectiveFrom) <= now()) {
    category.pointsPerKg = pointsPerKg
    category.ruleEffectiveFrom = effectiveFrom
  }
  return ok({
    ruleId: rule.ruleId,
    categoryId: category.categoryId,
    pointsPerKg,
    effectiveFrom
  })
})

route('GET', '/admin/categories/:categoryId/rules', ({ auth, params, query }) => {
  requireRole(auth, 'ADMIN')
  const category = findCategory(params.categoryId)
  if (!category) throw new MockError(1005, '类别不存在')
  const list = db.rules
    .filter((r) => r.categoryId === category.categoryId)
    .sort((a, b) => new Date(b.effectiveFrom) - new Date(a.effectiveFrom))
  return ok(paginate(list, query, 20))
})

/* ---------------------------- 管理员：站点 ---------------------------- */
route('POST', '/admin/sites', ({ auth, body }) => {
  requireRole(auth, 'ADMIN')
  const name = trim(body?.name)
  const address = trim(body?.address)
  if (!name || name.length > 30) throw new MockError(1001, '站点名称需为 1–30 字')
  if (!address || address.length > 100) throw new MockError(1001, '站点地址需为 1–100 字')
  const site = {
    siteId: nextId(),
    name,
    address,
    openTime: trim(body?.openTime) || null,
    manager: trim(body?.manager) || null,
    phone: trim(body?.phone) || null,
    status: 'ENABLED'
  }
  db.sites.push(site)
  return ok({ siteId: site.siteId })
})

route('PUT', '/admin/sites/:siteId', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const site = findSite(params.siteId)
  if (!site) throw new MockError(1005, '站点不存在')
  ;['name', 'address', 'openTime', 'manager', 'phone'].forEach((field) => {
    if (body?.[field] !== undefined && body[field] !== null) site[field] = trim(body[field])
  })
  return ok(site)
})

route('PATCH', '/admin/sites/:siteId/status', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const site = findSite(params.siteId)
  if (!site) throw new MockError(1005, '站点不存在')
  if (!['ENABLED', 'DISABLED'].includes(body?.status)) {
    throw new MockError(1001, '状态取值应为 ENABLED / DISABLED')
  }
  site.status = body.status
  return ok({ siteId: site.siteId, status: site.status })
})

/* ---------------------------- 管理员：账号 ---------------------------- */
route('GET', '/admin/accounts/users', ({ auth, query }) => {
  requireRole(auth, 'ADMIN')
  let users = db.users
  if (query.keyword) {
    const keyword = trim(query.keyword)
    users = users.filter(
      (u) =>
        u.nickname.includes(keyword) ||
        u.userCode.includes(keyword) ||
        u.phone.includes(keyword)
    )
  }
  if (query.status) users = users.filter((u) => u.status === query.status)
  const list = users.map(userAdminView)
  list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  return ok(paginate(list, query, 20))
})

route('PATCH', '/admin/accounts/users/:userId/status', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const user = findUser(params.userId)
  if (!user) throw new MockError(1005, '用户不存在')
  if (!['ACTIVE', 'DISABLED'].includes(body?.status)) {
    throw new MockError(1001, '状态取值应为 ACTIVE / DISABLED')
  }
  const reason = trim(body?.reason)
  if (body.status === 'DISABLED' && (!reason || reason.length > 200)) {
    throw new MockError(1001, '禁用时需填写 1–200 字的禁用原因')
  }
  user.status = body.status
  user.disabledReason = body.status === 'DISABLED' ? reason : null
  return ok({ userId: user.userId, status: user.status })
})

route('POST', '/admin/accounts/users/:userId/password/reset', ({ auth, params }) => {
  requireRole(auth, 'ADMIN')
  const user = findUser(params.userId)
  if (!user) throw new MockError(1005, '用户不存在')
  const tempPassword = randomPassword()
  user.password = tempPassword
  user.mustChangePassword = true
  return ok({
    tempPassword,
    expireAt: iso(new Date(now().getTime() + 24 * 3600 * 1000))
  })
})

route('GET', '/admin/accounts/recyclers', ({ auth, query }) => {
  requireRole(auth, 'ADMIN')
  let list = db.recyclers.map(recyclerView)
  if (query.status) list = list.filter((r) => r.status === query.status)
  if (query.keyword) {
    const keyword = trim(query.keyword)
    list = list.filter(
      (r) => r.name.includes(keyword) || r.phone.includes(keyword) || r.employeeNo.includes(keyword)
    )
  }
  list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  return ok(paginate(list, query, 20))
})

route('POST', '/admin/accounts/recyclers/:recyclerId/approve', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const recycler = findRecycler(params.recyclerId)
  if (!recycler) throw new MockError(1005, '回收员不存在')
  if (recycler.status !== 'PENDING') throw new MockError(1006, '仅待审核的申请可以通过')
  const site = findSite(body?.siteId)
  if (!site) throw new MockError(1005, '站点不存在')
  if (site.status !== 'ENABLED') throw new MockError(1001, '只能分配到启用状态的站点')
  recycler.status = 'APPROVED'
  recycler.siteId = site.siteId
  recycler.approvedBy = auth.id
  recycler.approvedAt = iso(now())
  return ok({ recyclerId: recycler.recyclerId, status: 'APPROVED', siteId: site.siteId, siteName: site.name })
})

route('POST', '/admin/accounts/recyclers/:recyclerId/reject', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const recycler = findRecycler(params.recyclerId)
  if (!recycler) throw new MockError(1005, '回收员不存在')
  if (recycler.status !== 'PENDING') throw new MockError(1006, '仅待审核的申请可以驳回')
  const reason = trim(body?.reason)
  if (!reason || reason.length < 10 || reason.length > 200) {
    throw new MockError(1001, '驳回原因需为 10–200 字')
  }
  recycler.status = 'REJECTED'
  recycler.rejectReason = reason
  recycler.approvedBy = auth.id
  recycler.approvedAt = iso(now())
  return ok({ recyclerId: recycler.recyclerId, status: 'REJECTED' })
})

route('PATCH', '/admin/accounts/recyclers/:recyclerId/status', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const recycler = findRecycler(params.recyclerId)
  if (!recycler) throw new MockError(1005, '回收员不存在')
  if (!['APPROVED', 'DISABLED'].includes(body?.status)) {
    throw new MockError(1001, '状态取值应为 APPROVED / DISABLED')
  }
  if (!['APPROVED', 'DISABLED'].includes(recycler.status)) {
    throw new MockError(1006, '当前状态不允许该操作')
  }
  recycler.status = body.status
  recycler.disabledReason = body.status === 'DISABLED' ? trim(body?.reason) || null : null
  return ok({ recyclerId: recycler.recyclerId, status: recycler.status })
})

route('POST', '/admin/accounts/recyclers/:recyclerId/password/reset', ({ auth, params }) => {
  requireRole(auth, 'ADMIN')
  const recycler = findRecycler(params.recyclerId)
  if (!recycler) throw new MockError(1005, '回收员不存在')
  const tempPassword = randomPassword()
  recycler.password = tempPassword
  recycler.mustChangePassword = true
  return ok({
    tempPassword,
    expireAt: iso(new Date(now().getTime() + 24 * 3600 * 1000))
  })
})

route('POST', '/admin/points/adjust', ({ auth, body }) => {
  requireRole(auth, 'ADMIN')
  const user = findUser(body?.userId)
  const change = num(body?.change)
  const reason = trim(body?.reason)
  if (!user) throw new MockError(1005, '用户不存在')
  if (!Number.isInteger(change) || change === 0 || Math.abs(change) > 100000) {
    throw new MockError(1001, '调整值需为非 0 整数且绝对值不超过 100000')
  }
  if (!reason || reason.length < 10 || reason.length > 200) {
    throw new MockError(1001, '调整原因需为 10–200 字')
  }
  const balanceBefore = user.pointsBalance
  const log = applyPoints(user, change, {
    type: 'ADJUST',
    bizType: 'ADMIN_ADJUST',
    bizNo: null,
    remark: reason,
    operatorType: 'ADMIN'
  })
  return ok({
    logId: log.logId,
    userId: user.userId,
    change,
    balanceBefore,
    balanceAfter: log.balanceAfter
  })
})

/* ---------------------------- 管理员：申诉 ---------------------------- */
route('GET', '/admin/appeals', ({ auth, query }) => {
  requireRole(auth, 'ADMIN')
  let list = clone(db.appeals)
  if (query.status) list = list.filter((a) => a.status === query.status)
  list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  return ok(paginate(list, query, 20))
})

route('POST', '/admin/appeals/:appealId/handle', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const appeal = db.appeals.find((a) => a.appealId === Number(params.appealId))
  if (!appeal) throw new MockError(1005, '申诉不存在')
  if (appeal.status !== 'PENDING') throw new MockError(1006, '该申诉已处理')
  const result = body?.result
  const conclusion = trim(body?.conclusion)
  if (!['ACCEPT', 'REJECT'].includes(result)) throw new MockError(1001, '处理结果取值应为 ACCEPT / REJECT')
  if (!conclusion || conclusion.length < 10 || conclusion.length > 500) {
    throw new MockError(1001, '处理结论需为 10–500 字')
  }
  appeal.status = result === 'ACCEPT' ? 'ACCEPTED' : 'REJECTED'
  appeal.conclusion = conclusion
  appeal.handledBy = findAdmin(auth.id)?.username ?? 'admin'
  appeal.handledAt = iso(now())
  return ok({ appealId: appeal.appealId, status: appeal.status, handledAt: appeal.handledAt })
})

/* ---------------------------- 管理员：公告 ---------------------------- */
route('GET', '/admin/announcements', ({ auth, query }) => {
  requireRole(auth, 'ADMIN')
  let list = clone(db.announcements)
  if (query.type) list = list.filter((a) => a.type === query.type)
  if (query.status) list = list.filter((a) => a.status === query.status)
  if (query.keyword) list = list.filter((a) => a.title.includes(trim(query.keyword)))
  list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  return ok(paginate(list, query, 20))
})

route('POST', '/admin/announcements', ({ auth, body }) => {
  requireRole(auth, 'ADMIN')
  const title = trim(body?.title)
  const type = body?.type
  const content = trim(body?.content)
  if (!title || title.length > 50) throw new MockError(1001, '标题需为 1–50 字')
  if (!['NOTICE', 'KNOWLEDGE'].includes(type)) throw new MockError(1001, '公告类型不合法')
  if (!content || content.length > 5000) throw new MockError(1001, '正文需为 1–5000 字')
  const status = body?.status === 'OFFLINE' ? 'OFFLINE' : 'PUBLISHED'
  const createdAt = iso(now())
  const item = {
    announcementId: nextId(),
    title,
    type,
    status,
    content,
    coverUrl: body?.coverUrl || null,
    summary: content.slice(0, 100),
    publishedAt: status === 'PUBLISHED' ? createdAt : null,
    createdAt,
    updatedAt: createdAt
  }
  db.announcements.unshift(item)
  return ok({ announcementId: item.announcementId })
})

route('PUT', '/admin/announcements/:announcementId', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const item = db.announcements.find((a) => a.announcementId === Number(params.announcementId))
  if (!item) throw new MockError(1005, '公告不存在')
  ;['title', 'type', 'content', 'coverUrl', 'status'].forEach((field) => {
    if (body?.[field] !== undefined && body[field] !== null) item[field] = body[field]
  })
  item.summary = item.content.slice(0, 100)
  if (item.status === 'PUBLISHED' && !item.publishedAt) item.publishedAt = iso(now())
  item.updatedAt = iso(now())
  return ok(item)
})

route('PATCH', '/admin/announcements/:announcementId/status', ({ auth, params, body }) => {
  requireRole(auth, 'ADMIN')
  const item = db.announcements.find((a) => a.announcementId === Number(params.announcementId))
  if (!item) throw new MockError(1005, '公告不存在')
  if (!['PUBLISHED', 'OFFLINE'].includes(body?.status)) {
    throw new MockError(1001, '状态取值应为 PUBLISHED / OFFLINE')
  }
  item.status = body.status
  if (body.status === 'PUBLISHED') item.publishedAt = iso(now())
  item.updatedAt = iso(now())
  return ok({ announcementId: item.announcementId, status: item.status, publishedAt: item.publishedAt })
})

route('DELETE', '/admin/announcements/:announcementId', ({ auth, params }) => {
  requireRole(auth, 'ADMIN')
  const exists = db.announcements.some((a) => a.announcementId === Number(params.announcementId))
  if (!exists) throw new MockError(1005, '公告不存在')
  db.announcements = db.announcements.filter(
    (a) => a.announcementId !== Number(params.announcementId)
  )
  return ok(null)
})

/* ---------------------------- 文件上传 ---------------------------- */
function readAsDataURL(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

route('POST', '/files', async ({ body }) => {
  const file = body?.get?.('file')
  const scene = body?.get?.('scene') || 'PRODUCT'
  if (!file) throw new MockError(1001, '请选择要上传的文件')
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    throw new MockError(4001, '仅支持 jpg / png / webp 图片')
  }
  if (file.size > 2 * 1024 * 1024) throw new MockError(4002, '文件超过 2 MB 限制')
  const url = await readAsDataURL(file)
  const record = {
    fileId: nextId(),
    url,
    size: file.size,
    mime: file.type,
    scene
  }
  db.files.push(record)
  return ok(record)
})

/* ------------------------------------------------------------------
 * 请求入口
 * ------------------------------------------------------------------ */
function parseAuth(headers = {}) {
  const raw = headers.Authorization || headers.authorization || ''
  const token = String(raw).replace(/^Bearer\s+/i, '')
  const match = /^mock\.(ADMIN|RECYCLER)\.(\d+)\./.exec(token)
  if (!match) return null
  return { role: match[1], id: Number(match[2]), token }
}

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

export async function mockRequest({ method, url, params = {}, data, headers = {} }) {
  await delay(120 + Math.random() * 180)

  const path = String(url).split('?')[0]
  const auth = parseAuth(headers)

  const routeDef = routes.find((r) => {
    if (r.method !== method.toUpperCase()) return false
    return r.pattern.test(path)
  })

  if (!routeDef) {
    return { code: 1005, message: `Mock 未实现的接口：${method} ${path}`, data: null }
  }

  const matches = routeDef.pattern.exec(path)
  const pathParams = {}
  routeDef.keys.forEach((key, index) => {
    pathParams[key] = decodeURIComponent(matches[index + 1])
  })

  try {
    const ctx = { params: pathParams, query: params || {}, body: data, auth }
    const result = await routeDef.handler(ctx)
    return result
  } catch (error) {
    if (error instanceof MockError) {
      return { code: error.code, message: error.message, data: null }
    }
    // eslint-disable-next-line no-console
    console.error('[mock] 未捕获异常', error)
    return { code: 5001, message: '系统内部错误（Mock）', data: null }
  }
}
