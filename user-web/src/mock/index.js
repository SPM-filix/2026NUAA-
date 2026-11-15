import { createSeedDb } from './data'

export const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'

const DB_KEY = 'nuaa.user.mock.v2'

function loadDb() {
  try {
    const raw = localStorage.getItem(DB_KEY)
    if (raw) return JSON.parse(raw)
  } catch {
    /* 损坏数据回退到种子数据 */
  }
  const db = createSeedDb()
  saveDb(db)
  return db
}

function saveDb(db) {
  localStorage.setItem(DB_KEY, JSON.stringify(db))
}

function currentUser(db) {
  return db.users.find((item) => item.userId === db.sessionUserId) || null
}

function mePayload(user) {
  return {
    userId: user.userId,
    userCode: user.userCode,
    phone: user.phone,
    nickname: user.nickname,
    avatarUrl: user.avatarUrl,
    address: user.address,
    pointsBalance: user.pointsBalance,
    status: user.status,
    createdAt: user.createdAt,
  }
}

function ok(data) {
  return { status: 200, data: { code: 0, message: 'ok', data } }
}

function fail(code, message) {
  return { status: 200, data: { code, message, data: null } }
}

function unauthorized() {
  return { status: 401, data: { code: 1002, message: '登录已过期，请重新登录', data: null } }
}

function paginate(rows, params, defaultSize = 10) {
  const page = Math.max(1, Number(params.page) || 1)
  const size = Math.min(50, Math.max(1, Number(params.size) || defaultSize))
  const start = (page - 1) * size
  return { total: rows.length, page, size, list: rows.slice(start, start + size) }
}

function isoAfter(ms) {
  const date = new Date(Date.now() + ms + 8 * 3600 * 1000)
  return date.toISOString().replace('Z', '+08:00')
}

function isoNow() {
  return isoAfter(0)
}

function todayCompact() {
  return isoNow().slice(0, 10).replace(/-/g, '')
}

function randomDigits(length) {
  let text = ''
  for (let i = 0; i < length; i += 1) text += Math.floor(Math.random() * 10)
  return text
}

function randomCode(length) {
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let text = ''
  for (let i = 0; i < length; i += 1) text += alphabet[Math.floor(Math.random() * alphabet.length)]
  return text
}

function nextId(rows, key) {
  return rows.reduce((max, item) => Math.max(max, Number(item[key]) || 0), 0) + 1
}

function sumWhere(rows, predicate) {
  return rows.filter(predicate).reduce((sum, item) => sum + item.change, 0)
}

function inRange(createdAt, from, to) {
  const time = new Date(createdAt).getTime()
  if (from && time < new Date(from).getTime()) return false
  if (to && time > new Date(to).getTime() + 24 * 3600 * 1000 - 1) return false
  return true
}

async function readFileAsDataUrl(file) {
  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = () => resolve('')
    reader.readAsDataURL(file)
  })
}

async function registerUser(db, body) {
  const phone = String(body.phone || '')
  const password = String(body.password || '')
  const nickname = String(body.nickname || '').trim()
  if (!/^1\d{10}$/.test(phone)) return fail(1001, '请输入 11 位手机号')
  if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,20}$/.test(password)) {
    return fail(1001, '密码需 8–20 位且同时包含字母与数字')
  }
  if (!nickname || nickname.length > 20) return fail(1001, '昵称需为 1–20 个字符')
  if (db.users.some((item) => item.phone === phone)) return fail(2003, '该手机号已注册')

  let userCode = randomDigits(6)
  while (db.users.some((item) => item.userCode === userCode)) userCode = randomDigits(6)

  const user = {
    userId: nextId(db.users, 'userId'),
    userCode,
    phone,
    password,
    nickname,
    avatarUrl: '',
    address: String(body.address || ''),
    pointsBalance: 0,
    status: 'ACTIVE',
    createdAt: isoNow(),
  }
  db.users.push(user)
  db.sessionUserId = user.userId
  saveDb(db)
  return ok({
    userId: user.userId,
    userCode: user.userCode,
    token: `mock-token-${user.userId}-${Date.now()}`,
    tokenType: 'Bearer',
    expiresIn: 7200,
  })
}

async function loginUser(db, body) {
  const user = db.users.find((item) => item.phone === String(body.phone || ''))
  if (!user || user.password !== String(body.password || '')) {
    return fail(2001, '账号或密码错误')
  }
  if (user.status !== 'ACTIVE') return fail(2002, '账号已被禁用')
  db.sessionUserId = user.userId
  saveDb(db)
  return ok({
    token: `mock-token-${user.userId}-${Date.now()}`,
    tokenType: 'Bearer',
    expiresIn: 7200,
    mustChangePassword: false,
    user: {
      userId: user.userId,
      nickname: user.nickname,
      avatarUrl: user.avatarUrl,
      pointsBalance: user.pointsBalance,
    },
  })
}

async function changePassword(db, user, body) {
  if (String(body.oldPassword || '') !== user.password) return fail(2005, '原密码错误')
  const newPassword = String(body.newPassword || '')
  if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,20}$/.test(newPassword)) {
    return fail(2006, '新密码需 8–20 位且同时包含字母与数字')
  }
  if (newPassword === user.password) return fail(2006, '新密码不得与原密码相同')
  user.password = newPassword
  saveDb(db)
  return ok(null)
}

async function exchange(db, user, body) {
  const key = String(body.idempotencyKey || '')
  if (!key) return fail(3014, '幂等键缺失，请重试')
  const cacheKey = `${user.userId}:${key}`
  if (db.idempotency[cacheKey]) return ok(db.idempotency[cacheKey])

  const product = db.products.find((item) => item.productId === Number(body.productId))
  if (!product) return fail(1005, '商品不存在')
  if (product.status !== 'ON') return fail(3003, '商品已下架')
  const quantity = Number(body.quantity) || 0
  const maxQuantity = Math.min(product.stock, 5)
  if (quantity < 1 || quantity > maxQuantity) return fail(3013, `兑换数量需在 1–${maxQuantity} 之间`)
  const pointsCost = product.pointsPrice * quantity
  if (user.pointsBalance < pointsCost) {
    return fail(3001, `积分不足，还差 ${pointsCost - user.pointsBalance} 积分`)
  }
  if (product.stock < quantity) return fail(3002, '商品库存不足')

  user.pointsBalance -= pointsCost
  product.stock -= quantity
  product.salesCount += quantity

  const orderId = nextId(db.orders, 'orderId')
  const order = {
    orderId,
    orderNo: `E${todayCompact()}${String(orderId).padStart(4, '0')}`,
    userId: user.userId,
    productId: product.productId,
    productName: product.name,
    productCoverUrl: product.coverUrl,
    quantity,
    pointsCost,
    status: 'PENDING',
    redeemCode: randomCode(8),
    redeemCodeExpireAt: isoAfter(30 * 24 * 3600 * 1000),
    siteName: null,
    verifierName: null,
    verifiedAt: null,
    cancelReason: null,
    createdAt: isoNow(),
  }
  db.orders.push(order)
  db.pointsLogs.push({
    logId: nextId(db.pointsLogs, 'logId'),
    type: 'EXPENSE',
    change: -pointsCost,
    balanceAfter: user.pointsBalance,
    bizType: 'EXCHANGE',
    bizNo: order.orderNo,
    remark: `兑换 ${product.name} × ${quantity}`,
    createdAt: order.createdAt,
  })

  const data = {
    orderId: order.orderId,
    orderNo: order.orderNo,
    redeemCode: order.redeemCode,
    redeemCodeExpireAt: order.redeemCodeExpireAt,
    productName: order.productName,
    quantity: order.quantity,
    pointsCost: order.pointsCost,
    balanceAfter: user.pointsBalance,
    status: order.status,
  }
  db.idempotency[cacheKey] = data
  saveDb(db)
  return ok(data)
}

async function createAppeal(db, user, body) {
  const targetType = String(body.targetType || '')
  const targetId = Number(body.targetId)
  const description = String(body.description || '').trim()
  if (!['DELIVERY', 'EXCHANGE'].includes(targetType)) return fail(1001, '申诉对象类型非法')
  if (description.length < 10 || description.length > 500) {
    return fail(1001, '问题描述需为 10–500 个字符')
  }

  if (targetType === 'DELIVERY') {
    const record = db.deliveries.find(
      (item) => item.recordId === targetId && item.userId === user.userId,
    )
    if (!record) return fail(1005, '投递记录不存在')
  } else {
    const order = db.orders.find(
      (item) => item.orderId === targetId && item.userId === user.userId,
    )
    if (!order) return fail(1005, '兑换订单不存在')
  }

  const appeal = {
    appealId: nextId(db.appeals, 'appealId'),
    userId: user.userId,
    targetType,
    targetId,
    targetNo:
      targetType === 'DELIVERY'
        ? db.deliveries.find((item) => item.recordId === targetId)?.recordNo
        : db.orders.find((item) => item.orderId === targetId)?.orderNo,
    description,
    status: 'PENDING',
    conclusion: null,
    handledAt: null,
    createdAt: isoNow(),
  }
  db.appeals.push(appeal)
  saveDb(db)
  return ok({ appealId: appeal.appealId, status: appeal.status, createdAt: appeal.createdAt })
}

function userExchanges(db, user, params) {
  const rows = db.orders
    .filter((item) => item.userId === user.userId)
    .filter((item) => !params.status || item.status === params.status)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .map((item) => ({
      orderId: item.orderId,
      orderNo: item.orderNo,
      productId: item.productId,
      productName: item.productName,
      productCoverUrl: item.productCoverUrl,
      quantity: item.quantity,
      pointsCost: item.pointsCost,
      status: item.status,
      redeemCode: item.redeemCode,
      createdAt: item.createdAt,
      verifiedAt: item.verifiedAt,
      cancelReason: item.cancelReason,
    }))
  return paginate(rows, params)
}

const rules = [
  {
    method: 'POST',
    path: /^\/api\/auth\/users\/register$/,
    handler: async ({ db, body }) => registerUser(db, body),
  },
  {
    method: 'POST',
    path: /^\/api\/auth\/users\/login$/,
    handler: async ({ db, body }) => loginUser(db, body),
  },
  {
    method: 'POST',
    path: /^\/api\/auth\/logout$/,
    handler: async ({ db }) => {
      db.sessionUserId = null
      saveDb(db)
      return ok(null)
    },
  },
  {
    method: 'PUT',
    path: /^\/api\/auth\/password$/,
    handler: async ({ db, user, body }) => {
      if (!user) return unauthorized()
      return changePassword(db, user, body)
    },
  },
  {
    method: 'GET',
    path: /^\/api\/users\/me$/,
    handler: async ({ user }) => (user ? ok(mePayload(user)) : unauthorized()),
  },
  {
    method: 'PUT',
    path: /^\/api\/users\/me$/,
    handler: async ({ db, user, body }) => {
      if (!user) return unauthorized()
      if (body.nickname !== undefined) {
        const nickname = String(body.nickname).trim()
        if (!nickname || nickname.length > 20) return fail(1001, '昵称需为 1–20 个字符')
        user.nickname = nickname
      }
      if (body.avatarUrl !== undefined) user.avatarUrl = String(body.avatarUrl)
      if (body.address !== undefined) {
        const address = String(body.address)
        if (address.length > 100) return fail(1001, '地址备注不能超过 100 个字符')
        user.address = address
      }
      saveDb(db)
      return ok(mePayload(user))
    },
  },
  {
    method: 'GET',
    path: /^\/api\/users\/me\/qrcode$/,
    handler: async ({ user }) => {
      if (!user) return unauthorized()
      return ok({
        userCode: user.userCode,
        qrContent: `NUAA-RECYCLE:U:${user.userId}-${Date.now()}`,
        expiresIn: 600,
        expiresAt: isoAfter(600 * 1000),
      })
    },
  },
  {
    method: 'GET',
    path: /^\/api\/users\/me\/points$/,
    handler: async ({ db, user }) => {
      if (!user) return unauthorized()
      const logs = db.pointsLogs.filter((item) => item.type !== undefined)
      return ok({
        balance: user.pointsBalance,
        totalIncome: sumWhere(logs, (item) => item.change > 0),
        totalExpense: Math.abs(sumWhere(logs, (item) => item.change < 0 && item.type !== 'REVOKE')),
        totalRevoked: Math.abs(sumWhere(logs, (item) => item.type === 'REVOKE')),
      })
    },
  },
  {
    method: 'GET',
    path: /^\/api\/users\/me\/points\/logs$/,
    handler: async ({ db, user, params }) => {
      if (!user) return unauthorized()
      const rows = db.pointsLogs
        .filter((item) => !params.type || item.type === params.type)
        .filter((item) => inRange(item.createdAt, params.from, params.to))
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      return ok(paginate(rows, params))
    },
  },
  {
    method: 'GET',
    path: /^\/api\/users\/me\/deliveries$/,
    handler: async ({ db, user, params }) => {
      if (!user) return unauthorized()
      const rows = db.deliveries
        .filter((item) => item.userId === user.userId)
        .filter((item) => !params.categoryId || item.categoryId === Number(params.categoryId))
        .filter((item) => inRange(item.createdAt, params.from, params.to))
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      return ok(paginate(rows, params))
    },
  },
  {
    method: 'GET',
    path: /^\/api\/users\/me\/exchanges\/(\d+)$/,
    handler: async ({ db, user, match }) => {
      if (!user) return unauthorized()
      const order = db.orders.find((item) => item.orderId === Number(match[1]))
      if (!order) return fail(1005, '订单不存在')
      if (order.userId !== user.userId) return fail(1003, '只能查看本人订单')
      return ok({ ...order, userId: order.userId })
    },
  },
  {
    method: 'GET',
    path: /^\/api\/users\/me\/exchanges$/,
    handler: async ({ db, user, params }) => {
      if (!user) return unauthorized()
      return ok(userExchanges(db, user, params))
    },
  },
  {
    method: 'POST',
    path: /^\/api\/users\/me\/appeals$/,
    handler: async ({ db, user, body }) => {
      if (!user) return unauthorized()
      return createAppeal(db, user, body)
    },
  },
  {
    method: 'GET',
    path: /^\/api\/users\/me\/appeals$/,
    handler: async ({ db, user, params }) => {
      if (!user) return unauthorized()
      const rows = db.appeals
        .filter((item) => item.userId === user.userId)
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      return ok(paginate(rows, params))
    },
  },
  {
    method: 'POST',
    path: /^\/api\/exchanges$/,
    handler: async ({ db, user, body }) => {
      if (!user) return unauthorized()
      return exchange(db, user, body)
    },
  },
  {
    method: 'GET',
    path: /^\/api\/products\/(\d+)$/,
    handler: async ({ db, match }) => {
      const product = db.products.find((item) => item.productId === Number(match[1]))
      if (!product) return fail(1005, '商品不存在')
      return ok(product)
    },
  },
  {
    method: 'GET',
    path: /^\/api\/products$/,
    handler: async ({ db, params }) => {
      let rows = db.products.filter((item) => item.status === 'ON')
      if (params.keyword) {
        const keyword = String(params.keyword).toLowerCase()
        rows = rows.filter((item) => item.name.toLowerCase().includes(keyword))
      }
      if (String(params.onlyAvailable) === 'true') rows = rows.filter((item) => item.stock > 0)
      if (params.sort === 'points_asc') rows.sort((a, b) => a.pointsPrice - b.pointsPrice)
      else if (params.sort === 'points_desc') rows.sort((a, b) => b.pointsPrice - a.pointsPrice)
      else rows.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      return ok(
        paginate(
          rows.map((item) => ({
            productId: item.productId,
            name: item.name,
            coverUrl: item.coverUrl,
            pointsPrice: item.pointsPrice,
            stock: item.stock,
            status: item.status,
            description: item.description,
          })),
          params,
        ),
      )
    },
  },
  {
    method: 'GET',
    path: /^\/api\/announcements\/(\d+)$/,
    handler: async ({ db, match }) => {
      const item = db.announcements.find(
        (row) => row.announcementId === Number(match[1]) && row.status === 'PUBLISHED',
      )
      if (!item) return fail(1005, '公告不存在')
      return ok(item)
    },
  },
  {
    method: 'GET',
    path: /^\/api\/announcements$/,
    handler: async ({ db, params }) => {
      const rows = db.announcements
        .filter((item) => item.status === 'PUBLISHED')
        .filter((item) => !params.type || item.type === params.type)
        .sort((a, b) => new Date(b.publishedAt) - new Date(a.publishedAt))
        .map((item) => ({
          announcementId: item.announcementId,
          title: item.title,
          type: item.type,
          summary: item.content.slice(0, 100),
          coverUrl: item.coverUrl,
          publishedAt: item.publishedAt,
        }))
      return ok(paginate(rows, params))
    },
  },
  {
    method: 'GET',
    path: /^\/api\/categories$/,
    handler: async ({ db, params }) => {
      let rows = db.categories
      if (String(params.enabledOnly) === 'true') rows = rows.filter((item) => item.status === 'ENABLED')
      return ok(rows.slice().sort((a, b) => a.sort - b.sort))
    },
  },
  {
    method: 'POST',
    path: /^\/api\/files$/,
    handler: async ({ db, user, body }) => {
      if (!user) return unauthorized()
      const file = body.get?.('file')
      const scene = body.get?.('scene') || 'AVATAR'
      if (!file) return fail(1001, '请选择文件')
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
        return fail(4001, '文件类型不支持，仅支持 jpg / png / webp')
      }
      if (file.size > 2 * 1024 * 1024) return fail(4002, '文件超过 2 MB 限制')
      const url = await readFileAsDataUrl(file)
      return ok({
        fileId: Date.now(),
        url,
        size: file.size,
        mime: file.type,
        scene,
      })
    },
  },
]

function dispatch(method, path, params, body) {
  const db = loadDb()
  const user = currentUser(db)
  const ctx = { db, user, params, body: body || {} }
  const rule = rules.find((item) => item.method === method && item.path.test(path))
  if (!rule) return fail(1005, `Mock 未实现该接口：${method} ${path}`)
  ctx.match = path.match(rule.path)
  return rule.handler(ctx)
}

export function mockAdapter(config) {
  const method = (config.method || 'get').toUpperCase()
  const rawUrl = `${config.baseURL || ''}${config.url || ''}`
  const path = rawUrl.split('?')[0].replace(/^https?:\/\/[^/]+/, '')
  const params = config.params || {}
  let body = config.data
  if (typeof body === 'string') {
    try {
      body = JSON.parse(body)
    } catch {
      body = {}
    }
  }

  return new Promise((resolve) => {
    setTimeout(async () => {
      let result
      try {
        result = await dispatch(method, path, params, body)
      } catch (error) {
        result = fail(5001, '系统内部错误')
      }
      resolve({
        data: result.data,
        status: result.status,
        statusText: 'OK',
        headers: {},
        config,
        request: {},
      })
    }, 180)
  })
}
