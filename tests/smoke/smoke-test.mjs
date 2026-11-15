#!/usr/bin/env node
/**
 * 阶段③后端冒烟脚本：按主流程串起认证、投递、兑换、核销、统计、越权。
 *
 * 用法：
 *   SMOKE_BASE=http://127.0.0.1:8080 ADMIN_INIT_PASSWORD=Admin12345 node tests/smoke/smoke-test.mjs
 *
 * 前置：后端已启动、Flyway 已建表、V2 种子数据存在、ADMIN_INIT_PASSWORD 与启动时一致。
 * 每次运行会注册新的用户与回收员，重复运行不会冲突。
 */

const BASE = process.env.SMOKE_BASE || 'http://127.0.0.1:8080'
const ADMIN_PASSWORD = process.env.ADMIN_INIT_PASSWORD || 'Admin12345'
const USER_PASSWORD = 'abc12345'
const RECYCLER_PASSWORD = 'recycler123'
const SITE_ID = 1
const DELIVERY_CATEGORY_ID = 2
const DELIVERY_WEIGHT = 10.5
const EXPECTED_DELIVERY_POINTS = 158

let passed = 0
let failed = 0
const failures = []

function check(name, condition, detail = '') {
  if (condition) {
    passed += 1
    console.log(`  PASS  ${name}`)
  } else {
    failed += 1
    failures.push(`${name}${detail ? ` — ${detail}` : ''}`)
    console.log(`  FAIL  ${name}${detail ? ` — ${detail}` : ''}`)
  }
}

async function call(method, path, { token, body } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  let json = null
  try {
    json = await res.json()
  } catch {
    // 非 JSON 响应保持 null
  }
  return { status: res.status, json }
}

function uniquePhone(prefix) {
  const tail = String(Date.now()).slice(-8)
  return `${prefix}${tail}`
}

async function main() {
  console.log(`Smoke target: ${BASE}`)

  // 0. 健康检查
  const health = await call('GET', '/api/health')
  check('健康检查 /api/health', health.status === 200 && health.json?.data?.status === 'UP')

  // 1. 管理员登录
  const adminLogin = await call('POST', '/api/auth/admins/login', {
    body: { username: 'admin', password: ADMIN_PASSWORD }
  })
  const adminToken = adminLogin.json?.data?.token
  check('管理员登录', adminLogin.status === 200 && Boolean(adminToken), JSON.stringify(adminLogin.json))
  if (!adminToken) return

  // 2. 用户注册（注册即登录）
  const phone = uniquePhone('139')
  const userReg = await call('POST', '/api/auth/users/register', {
    body: { phone, password: USER_PASSWORD, nickname: '冒烟用户', address: '测试楼 101' }
  })
  const userToken = userReg.json?.data?.token
  const userId = userReg.json?.data?.userId
  const userCode = userReg.json?.data?.userCode
  check('用户注册', userReg.status === 200 && Boolean(userToken) && Boolean(userId), JSON.stringify(userReg.json))
  check('用户注册返回 6 位编号', typeof userCode === 'string' && /^\d{6}$/.test(userCode), String(userCode))
  if (!userToken) return

  // 3. 用户本人信息与二维码
  const me = await call('GET', '/api/users/me', { token: userToken })
  check('用户本人信息', me.status === 200 && me.json?.data?.userId === userId)
  const qr = await call('GET', '/api/users/me/qrcode', { token: userToken })
  check('身份二维码', qr.status === 200 && String(qr.json?.data?.qrContent || '').startsWith('NUAA-RECYCLE:U:'),
    JSON.stringify(qr.json))

  // 4. 回收员注册 + 管理员审核
  const recyclerPhone = uniquePhone('138')
  const recyclerReg = await call('POST', '/api/auth/recyclers/register', {
    body: {
      name: '冒烟回收员',
      phone: recyclerPhone,
      password: RECYCLER_PASSWORD,
      employeeNo: `E${String(Date.now()).slice(-6)}`,
      siteId: SITE_ID,
      reason: '阶段③冒烟测试'
    }
  })
  const recyclerId = recyclerReg.json?.data?.recyclerId
  check('回收员注册', recyclerReg.status === 200 && Boolean(recyclerId), JSON.stringify(recyclerReg.json))
  if (!recyclerId) return

  const approve = await call('POST', `/api/admin/accounts/recyclers/${recyclerId}/approve`, {
    token: adminToken,
    body: { siteId: SITE_ID }
  })
  check('管理员审核回收员', approve.status === 200 && approve.json?.data?.status === 'APPROVED', JSON.stringify(approve.json))

  const recyclerLogin = await call('POST', '/api/auth/recyclers/login', {
    body: { phone: recyclerPhone, password: RECYCLER_PASSWORD }
  })
  const recyclerToken = recyclerLogin.json?.data?.token
  check('回收员登录', recyclerLogin.status === 200 && Boolean(recyclerToken), JSON.stringify(recyclerLogin.json))
  if (!recyclerToken) return

  // 5. 识别用户 + 投递登记
  const lookupByPhone = await call('GET', `/api/users/lookup?keyword=${phone}`, { token: recyclerToken })
  check('手机号识别用户', lookupByPhone.status === 200 && lookupByPhone.json?.data?.userId === userId)
  check('识别结果手机号脱敏', /^\d{3}\*{4}\d{4}$/.test(lookupByPhone.json?.data?.phoneMasked || ''))

  const delivery = await call('POST', '/api/deliveries', {
    token: recyclerToken,
    body: { userId, categoryId: DELIVERY_CATEGORY_ID, weight: DELIVERY_WEIGHT, remark: '冒烟测试' }
  })
  const deliveryData = delivery.json?.data
  check('投递登记', delivery.status === 200 && Boolean(deliveryData?.recordId), JSON.stringify(delivery.json))
  check('积分按规则计算（15 分/kg × 10.5kg = 158）', deliveryData?.points === EXPECTED_DELIVERY_POINTS, String(deliveryData?.points))

  const points = await call('GET', '/api/users/me/points', { token: userToken })
  check('用户积分到账', points.status === 200 && points.json?.data?.balance === EXPECTED_DELIVERY_POINTS, JSON.stringify(points.json))

  // 6. 商城兑换（幂等）
  const products = await call('GET', '/api/products?onlyAvailable=true&size=50', { token: userToken })
  const product = (products.json?.data?.list || []).find(
    (item) => item.status === 'ON' && item.pointsPrice <= EXPECTED_DELIVERY_POINTS && item.stock >= 1
  )
  check('商品列表可兑换', Boolean(product), JSON.stringify(products.json?.data?.list))
  if (!product) return

  const idemKey = `smoke-${Date.now()}`
  const order = await call('POST', '/api/exchanges', {
    token: userToken,
    body: { productId: product.id, quantity: 1, idempotencyKey: idemKey }
  })
  const orderData = order.json?.data
  check('积分兑换', order.status === 200 && Boolean(orderData?.orderId), JSON.stringify(order.json))
  check('兑换后余额扣减', orderData?.balanceAfter === EXPECTED_DELIVERY_POINTS - product.pointsPrice, String(orderData?.balanceAfter))

  const orderAgain = await call('POST', '/api/exchanges', {
    token: userToken,
    body: { productId: product.id, quantity: 1, idempotencyKey: idemKey }
  })
  check('同一幂等键返回首笔订单', orderAgain.json?.data?.orderId === orderData?.orderId, JSON.stringify(orderAgain.json))

  // 7. 核销（一次性）
  const redeem = await call('POST', '/api/exchanges/redeem', {
    token: recyclerToken,
    body: { redeemCode: orderData?.redeemCode }
  })
  check('回收员核销', redeem.status === 200 && redeem.json?.data?.status === 'COMPLETED', JSON.stringify(redeem.json))

  const redeemAgain = await call('POST', '/api/exchanges/redeem', {
    token: recyclerToken,
    body: { redeemCode: orderData?.redeemCode }
  })
  check('重复核销被拒绝（3005）', redeemAgain.json?.code === 3005, JSON.stringify(redeemAgain.json))

  // 8. 回收员数据隔离与工作台
  const myRecords = await call('GET', '/api/recyclers/me/deliveries', { token: recyclerToken })
  check('本人流水只含本人数据', myRecords.status === 200 &&
    (myRecords.json?.data?.list || []).every((item) => item.recyclerId === recyclerId))

  const workbench = await call('GET', '/api/recyclers/me/workbench', { token: recyclerToken })
  check('回收员工作台今日重量', workbench.status === 200 && Number(workbench.json?.data?.todayWeight) === DELIVERY_WEIGHT,
    JSON.stringify(workbench.json))
  check('回收员工作台今日核销数', workbench.json?.data?.todayRedeemCount === 1, JSON.stringify(workbench.json))
  check('回收员工作台本月排名', workbench.json?.data?.monthRank === 1, JSON.stringify(workbench.json))

  // 9. 统计聚合（管理端）
  const overview = await call('GET', '/api/admin/stats/overview', { token: adminToken })
  check('统计总览重量', overview.status === 200 && Number(overview.json?.data?.totalWeight) >= DELIVERY_WEIGHT,
    JSON.stringify(overview.json))
  check('统计总览发放积分', Number(overview.json?.data?.totalPointsIssued) >= EXPECTED_DELIVERY_POINTS, JSON.stringify(overview.json))
  check('统计总览核销积分', Number(overview.json?.data?.totalPointsRedeemed) >= product.pointsPrice, JSON.stringify(overview.json))

  const categories = await call('GET', '/api/admin/stats/categories', { token: adminToken })
  const plastic = (categories.json?.data || []).find((item) => item.categoryId === DELIVERY_CATEGORY_ID)
  check('分类占比有数据', categories.status === 200 && Boolean(plastic), JSON.stringify(categories.json))
  check('分类占比合计为 1 左右', Math.abs((categories.json?.data || []).reduce((sum, item) => sum + item.weightRatio, 0) - 1) < 0.02,
    JSON.stringify(categories.json?.data))

  const trend = await call('GET', '/api/admin/stats/trend?days=7', { token: adminToken })
  const today = new Date().toISOString().slice(0, 10)
  const todayRow = (trend.json?.data || []).find((item) => item.date === today)
  check('趋势返回 7 天', trend.status === 200 && trend.json?.data?.length === 7)
  check('趋势今日有投递', Boolean(todayRow) && todayRow.deliveryCount >= 1, JSON.stringify(todayRow))

  const topProducts = await call('GET', '/api/admin/stats/top?type=PRODUCT&limit=5', { token: adminToken })
  check('商品排行含已核销商品', (topProducts.json?.data || []).some((item) => item.id === product.id), JSON.stringify(topProducts.json))

  const topUsers = await call('GET', '/api/admin/stats/top?type=USER&limit=5', { token: adminToken })
  check('用户排行昵称脱敏', (topUsers.json?.data || []).every((item) => !item.name || item.name.endsWith('**')),
    JSON.stringify(topUsers.json))

  // 10. 越权
  const userOnAdmin = await call('GET', '/api/admin/stats/overview', { token: userToken })
  check('用户调用管理员接口被拒', userOnAdmin.status === 403 || userOnAdmin.json?.code === 1003, JSON.stringify(userOnAdmin.json))

  const adminOnDelivery = await call('POST', '/api/deliveries', {
    token: adminToken,
    body: { userId, categoryId: DELIVERY_CATEGORY_ID, weight: 1 }
  })
  check('管理员调用投递接口被拒', adminOnDelivery.status === 403 || adminOnDelivery.json?.code === 1003,
    JSON.stringify(adminOnDelivery.json))

  console.log(`\n结果：${passed} 通过，${failed} 失败`)
  if (failed > 0) {
    console.log('失败项：')
    failures.forEach((item) => console.log(` - ${item}`))
    process.exitCode = 1
  }
}

main().catch((error) => {
  console.error('冒烟脚本异常：', error)
  process.exitCode = 1
})
