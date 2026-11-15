#!/usr/bin/env node
/**
 * 阶段③高并发测试（生产环境）。
 *
 * 场景：
 *   S1 库存抢购：50 用户并发兑换库存为 1 的商品，期望恰好 1 单成功，不超卖
 *   S2 幂等键并发：同一用户 10 个相同 idempotencyKey 并发，期望只成交 1 单、只扣一次积分
 *   S3 核销并发：10 个相同核销码并发核销，期望只成功 1 次
 *   S4 积分扣减：余额 30、商品 20 分，5 个不同幂等键并发，期望只成功 1 单，余额不为负
 *
 * 用法：
 *   SMOKE_BASE=http://8.136.245.40 ADMIN_INIT_PASSWORD=*** node tests/load/high-concurrency-test.mjs
 */

const BASE = process.env.SMOKE_BASE || 'http://127.0.0.1:8080'
const ADMIN_PASSWORD = process.env.ADMIN_INIT_PASSWORD
const RECYCLER_PHONE = process.env.RECYCLER_PHONE || '13700000001'
const RECYCLER_PASSWORD = process.env.RECYCLER_PASSWORD || 'recycler123'
const USER_PASSWORD = 'abc12345'

if (!ADMIN_PASSWORD) {
  console.error('缺少 ADMIN_INIT_PASSWORD')
  process.exit(1)
}

async function call(method, path, { token, body } = {}) {
  const started = performance.now()
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`
  const response = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  let json = null
  try {
    json = await response.json()
  } catch {
    // ignore
  }
  return { status: response.status, json, ms: performance.now() - started }
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

function percentile(values, p) {
  if (values.length === 0) return 0
  const sorted = [...values].sort((a, b) => a - b)
  const index = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1)
  return Number(sorted[index].toFixed(1))
}

function stats(results, wallMs) {
  const durations = results.map((item) => item.ms)
  const total = durations.reduce((sum, value) => sum + value, 0)
  return {
    count: durations.length,
    p50: percentile(durations, 50),
    p95: percentile(durations, 95),
    max: Number(Math.max(...durations).toFixed(1)),
    avg: Number((total / durations.length).toFixed(1)),
    wallMs: Number(wallMs.toFixed(1)),
    burstRps: Number((durations.length / (wallMs / 1000)).toFixed(1))
  }
}

let passed = 0
let failed = 0
function check(name, ok, detail = '') {
  console.log(`  ${ok ? 'PASS' : 'FAIL'}  ${name}${ok || !detail ? '' : ` — ${detail}`}`)
  if (ok) passed += 1
  else failed += 1
}

async function registerUser(index) {
  const phone = `159${String(Date.now()).slice(-5)}${String(index).padStart(3, '0')}`
  const response = await call('POST', '/api/auth/users/register', {
    body: { phone, password: USER_PASSWORD, nickname: `并发用户${index}` }
  })
  const data = response.json?.data
  if (!data?.token) throw new Error(`注册失败：${JSON.stringify(response.json)}`)
  return { phone, token: data.token, userId: data.userId }
}

async function main() {
  console.log(`高并发测试目标：${BASE}`)
  console.log(`时间：${new Date().toISOString()}`)

  const adminLogin = await call('POST', '/api/auth/admins/login', {
    body: { username: 'admin', password: ADMIN_PASSWORD }
  })
  const adminToken = adminLogin.json?.data?.token
  if (!adminToken) throw new Error(`管理员登录失败：${JSON.stringify(adminLogin.json)}`)

  const recyclerLogin = await call('POST', '/api/auth/recyclers/login', {
    body: { phone: RECYCLER_PHONE, password: RECYCLER_PASSWORD }
  })
  const recyclerToken = recyclerLogin.json?.data?.token
  if (!recyclerToken) throw new Error(`回收员登录失败：${JSON.stringify(recyclerLogin.json)}`)

  // 测试商品：一个库存 1（S1/S3），一个库存 10（S2/S4）
  const stockProduct = await call('POST', '/api/admin/products', {
    token: adminToken,
    body: { name: `并发测验-库存1-${Date.now()}`, pointsPrice: 10, stock: 1, spec: '测试' }
  })
  const stockProductId = stockProduct.json?.data?.productId
  if (!stockProductId) throw new Error(`创建库存商品失败：${JSON.stringify(stockProduct.json)}`)

  const pointsProduct = await call('POST', '/api/admin/products', {
    token: adminToken,
    body: { name: `并发测验-扣分-${Date.now()}`, pointsPrice: 20, stock: 10, spec: '测试' }
  })
  const pointsProductId = pointsProduct.json?.data?.productId
  if (!pointsProductId) throw new Error(`创建扣分商品失败：${JSON.stringify(pointsProduct.json)}`)

  try {
    // ---------- S1 库存抢购 ----------
    console.log('\n[场景 1] 50 用户并发兑换库存 1 的商品')
    const users = []
    for (let i = 0; i < 50; i += 1) {
      users.push(await registerUser(i))
    }
    for (const user of users) {
      await call('POST', '/api/admin/points/adjust', {
        token: adminToken,
        body: { userId: user.userId, change: 100, reason: '高并发测试充值' }
      })
    }
    check('50 个用户均注册并充值 100 分', users.length === 50)

    const stamp = Date.now()
    const s1Started = performance.now()
    const s1 = await Promise.all(
      users.map((user, index) =>
        call('POST', '/api/exchanges', {
          token: user.token,
          body: { productId: stockProductId, quantity: 1, idempotencyKey: `s1-${stamp}-${index}` }
        })
      )
    )
    const s1Wall = performance.now() - s1Started
    const s1Success = s1.filter((item) => item.json?.code === 0 && item.json?.data?.orderId)
    const s1Stock = s1.filter((item) => item.json?.code === 3002).length
    const s1Other = s1.filter((item) => ![0, 3002].includes(item.json?.code))
    const stockAfter = await call('GET', `/api/products/${stockProductId}`, { token: adminToken })

    check('恰好 1 单成功', s1Success.length === 1, `实际 ${s1Success.length}`)
    check('其余为库存不足 3002', s1Stock === 49, `3002=${s1Stock}，其他=${s1Other.length}`)
    check('库存归零未超卖', stockAfter.json?.data?.stock === 0, String(stockAfter.json?.data?.stock))
    check('无 5xx 或未知错误', s1Other.length === 0, JSON.stringify(s1Other.map((item) => item.json)))
    console.log('  延迟：', stats(s1, s1Wall))

    // ---------- S3 核销并发（使用 S1 成功订单） ----------
    console.log('\n[场景 3] 同一核销码 10 并发核销')
    const redeemCode = s1Success[0]?.json?.data?.redeemCode
    const s3Started = performance.now()
    const s3 = await Promise.all(
      Array.from({ length: 10 }, () =>
        call('POST', '/api/exchanges/redeem', { token: recyclerToken, body: { redeemCode } })
      )
    )
    const s3Wall = performance.now() - s3Started
    const s3Success = s3.filter((item) => item.json?.code === 0 && item.json?.data?.status === 'COMPLETED')
    const s3Invalid = s3.filter((item) => item.json?.code === 3005)
    check('恰好 1 次核销成功', s3Success.length === 1, `实际 ${s3Success.length}`)
    check('其余返回 3005', s3Invalid.length === 9, `3005=${s3Invalid.length}`)
    check('无 5xx 或未知错误', s3.every((item) => [0, 3005].includes(item.json?.code)),
      JSON.stringify(s3.map((item) => item.json?.code)))
    console.log('  延迟：', stats(s3, s3Wall))

    // ---------- S2 同一幂等键并发 ----------
    console.log('\n[场景 2] 同一用户同一幂等键 10 并发')
    const idemUser = users[0]
    await call('POST', '/api/admin/points/adjust', {
      token: adminToken,
      body: { userId: idemUser.userId, change: 100, reason: '幂等测试充值' }
    })
    const beforeBalance = (await call('GET', '/api/users/me/points', { token: idemUser.token }))
      .json?.data?.balance
    const idemKey = `s2-${Date.now()}`
    const s2Started = performance.now()
    const s2 = await Promise.all(
      Array.from({ length: 10 }, () =>
        call('POST', '/api/exchanges', {
          token: idemUser.token,
          body: { productId: pointsProductId, quantity: 1, idempotencyKey: idemKey }
        })
      )
    )
    const s2Wall = performance.now() - s2Started
    const s2Orders = new Set(s2.filter((item) => item.json?.code === 0).map((item) => item.json?.data?.orderId))
    const afterBalance = (await call('GET', '/api/users/me/points', { token: idemUser.token }))
      .json?.data?.balance
    const idemOrders = await call('GET', '/api/users/me/exchanges?size=50', { token: idemUser.token })
    const idemCount = (idemOrders.json?.data?.list || []).filter((item) => item.productId === pointsProductId).length

    check('10 个请求返回同一订单号', s2Orders.size === 1, `订单号数量 ${s2Orders.size}`)
    check('只扣一次积分', afterBalance === beforeBalance - 20, `${beforeBalance} -> ${afterBalance}`)
    check('该商品订单数为 1', idemCount === 1, String(idemCount))
    check('无 5xx', s2.every((item) => item.json?.code === 0), JSON.stringify(s2.map((item) => item.json?.code)))
    console.log('  延迟：', stats(s2, s2Wall))

    // ---------- S4 积分并发扣减 ----------
    console.log('\n[场景 4] 余额 30、单价 20，5 个不同幂等键并发')
    const pointsUser = await registerUser(999)
    await call('POST', '/api/admin/points/adjust', {
      token: adminToken,
      body: { userId: pointsUser.userId, change: 30, reason: '积分并发测试' }
    })
    const s4Started = performance.now()
    const s4 = await Promise.all(
      Array.from({ length: 5 }, (_, index) =>
        call('POST', '/api/exchanges', {
          token: pointsUser.token,
          body: { productId: pointsProductId, quantity: 1, idempotencyKey: `s4-${Date.now()}-${index}` }
        })
      )
    )
    const s4Wall = performance.now() - s4Started
    const s4Success = s4.filter((item) => item.json?.code === 0)
    const balanceAfter4 = (await call('GET', '/api/users/me/points', { token: pointsUser.token }))
      .json?.data?.balance

    check('只成功 1 单', s4Success.length === 1, `实际 ${s4Success.length}`)
    check('余额为 10 且未扣成负数', balanceAfter4 === 10, String(balanceAfter4))
    check('其余为积分不足 3001', s4.filter((item) => item.json?.code === 3001).length === 4,
      JSON.stringify(s4.map((item) => item.json?.code)))
    console.log('  延迟：', stats(s4, s4Wall))
  } finally {
    // 测试商品有订单不能删除，统一下架
    await call('PATCH', `/api/admin/products/${stockProductId}/status`, {
      token: adminToken,
      body: { status: 'OFF' }
    })
    await call('PATCH', `/api/admin/products/${pointsProductId}/status`, {
      token: adminToken,
      body: { status: 'OFF' }
    })
  }

  console.log(`\n结果：${passed} 通过，${failed} 失败`)
  if (failed > 0) process.exitCode = 1
}

main().catch((error) => {
  console.error('并发测试异常：', error)
  process.exitCode = 1
})
