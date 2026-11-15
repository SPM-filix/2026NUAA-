#!/usr/bin/env node
/**
 * 阶段③并发用例 TC-exchange-001：库存为 1 时 20 并发兑换。
 *
 * 期望：只有 1 笔成功；库存归零；用户积分只扣一次；其余请求返回业务错误（3002/3001），不出现 500。
 *
 * 用法：
 *   SMOKE_BASE=http://127.0.0.1:8080 ADMIN_INIT_PASSWORD=Admin12345 node tests/smoke/concurrency-test.mjs
 */

const BASE = process.env.SMOKE_BASE || 'http://127.0.0.1:8080'
const ADMIN_PASSWORD = process.env.ADMIN_INIT_PASSWORD || 'Admin12345'
const USER_PASSWORD = 'abc12345'
const RECYCLER_PASSWORD = 'recycler123'
const SITE_ID = 1
const CATEGORY_ID = 2
const WEIGHT = 3
const EXPECTED_POINTS = 45
const CONCURRENCY = 20

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
    // ignore
  }
  return { status: res.status, json }
}

const uniquePhone = (prefix) => `${prefix}${String(Date.now()).slice(-8)}`

async function setup() {
  const admin = await call('POST', '/api/auth/admins/login', {
    body: { username: 'admin', password: ADMIN_PASSWORD }
  })
  const adminToken = admin.json?.data?.token
  if (!adminToken) throw new Error(`管理员登录失败：${JSON.stringify(admin.json)}`)

  const phone = uniquePhone('137')
  const userReg = await call('POST', '/api/auth/users/register', {
    body: { phone, password: USER_PASSWORD, nickname: '并发用户' }
  })
  const userToken = userReg.json?.data?.token
  const userId = userReg.json?.data?.userId
  if (!userToken) throw new Error(`用户注册失败：${JSON.stringify(userReg.json)}`)

  const recyclerPhone = uniquePhone('136')
  const recyclerReg = await call('POST', '/api/auth/recyclers/register', {
    body: {
      name: '并发回收员',
      phone: recyclerPhone,
      password: RECYCLER_PASSWORD,
      employeeNo: `C${String(Date.now()).slice(-6)}`,
      siteId: SITE_ID,
      reason: '并发用例'
    }
  })
  const recyclerId = recyclerReg.json?.data?.recyclerId
  if (!recyclerId) throw new Error(`回收员注册失败：${JSON.stringify(recyclerReg.json)}`)

  await call('POST', `/api/admin/accounts/recyclers/${recyclerId}/approve`, {
    token: adminToken,
    body: { siteId: SITE_ID }
  })
  const recyclerLogin = await call('POST', '/api/auth/recyclers/login', {
    body: { phone: recyclerPhone, password: RECYCLER_PASSWORD }
  })
  const recyclerToken = recyclerLogin.json?.data?.token
  if (!recyclerToken) throw new Error(`回收员登录失败：${JSON.stringify(recyclerLogin.json)}`)

  const delivery = await call('POST', '/api/deliveries', {
    token: recyclerToken,
    body: { userId, categoryId: CATEGORY_ID, weight: WEIGHT, remark: '并发用例' }
  })
  if (delivery.json?.data?.points !== EXPECTED_POINTS) {
    throw new Error(`投递加点异常：${JSON.stringify(delivery.json)}`)
  }

  return { adminToken, userToken, userId }
}

async function main() {
  console.log(`Concurrency target: ${BASE}`)
  const { userToken, userId } = await setup()

  // 选一件库存为 1 的商品
  const products = await call('GET', '/api/products?onlyAvailable=true&size=50', { token: userToken })
  const product = (products.json?.data?.list || []).find((item) => item.stock === 1)
  if (!product) throw new Error('没有库存为 1 的在售商品，无法执行并发库存用例')
  console.log(`商品：${product.name}（id=${product.id}，库存=${product.stock}，价格=${product.pointsPrice}）`)

  const before = await call('GET', '/api/users/me/points', { token: userToken })
  const balanceBefore = before.json?.data?.balance
  console.log(`并发前余额：${balanceBefore}`)

  const stamp = Date.now()
  const results = await Promise.all(
    Array.from({ length: CONCURRENCY }, (_, index) =>
      call('POST', '/api/exchanges', {
        token: userToken,
        body: { productId: product.id, quantity: 1, idempotencyKey: `conc-${stamp}-${index}` }
      })
    )
  )

  const succeeded = results.filter((item) => item.json?.code === 0 && item.json?.data?.orderId)
  const codes = results.map((item) => item.json?.code ?? item.status)
  const stockFailures = codes.filter((code) => code === 3002).length
  const balanceFailures = codes.filter((code) => code === 3001).length
  const unexpected = results.filter((item) => item.status >= 500 || ![0, 3001, 3002].includes(item.json?.code))

  const detail = await call('GET', `/api/products/${product.id}`, { token: userToken })
  const after = await call('GET', '/api/users/me/points', { token: userToken })
  const orders = await call('GET', '/api/users/me/exchanges?size=50', { token: userToken })
  const myOrders = (orders.json?.data?.list || []).filter((item) => item.productId === product.id)

  console.log('\n=== 结果 ===')
  console.log(`成功下单：${succeeded.length} / ${CONCURRENCY}`)
  console.log(`库存不足 3002：${stockFailures}，积分不足 3001：${balanceFailures}`)
  console.log(`商品库存：${product.stock} -> ${detail.json?.data?.stock}`)
  console.log(`用户余额：${balanceBefore} -> ${after.json?.data?.balance}`)
  console.log(`该商品订单数：${myOrders.length}`)

  let failed = 0
  const check = (name, ok, detailText = '') => {
    console.log(`${ok ? '  PASS' : '  FAIL'}  ${name}${ok || !detailText ? '' : ` — ${detailText}`}`)
    if (!ok) failed += 1
  }
  check('恰好 1 笔兑换成功', succeeded.length === 1, `实际 ${succeeded.length}`)
  check('库存归零且未超卖', detail.json?.data?.stock === 0, String(detail.json?.data?.stock))
  check('积分只扣一次', after.json?.data?.balance === balanceBefore - product.pointsPrice,
    `${balanceBefore} -> ${after.json?.data?.balance}`)
  check('只生成 1 笔订单', myOrders.length === 1, `实际 ${myOrders.length}`)
  check('无 500 等异常响应', unexpected.length === 0, JSON.stringify(unexpected.map((item) => item.json)))

  if (failed > 0) process.exitCode = 1
}

main().catch((error) => {
  console.error('并发脚本异常：', error)
  process.exitCode = 1
})
