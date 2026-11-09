# 接口契约（API Contract）

| 项目 | 内容 |
| --- | --- |
| 文档版本 | **v0.1（草案，待阶段①评审冻结）** |
| 负责人 | 邓裕达（认证、投递、兑换、核销）／陈益苇（商品、类别规则、账号、统计、上传、公告）｜**本稿由魏鸿按需求文档起草，待两位后端负责人逐条确认** |
| 维护 | 接口变更**必须**在同一个 PR 内更新本文件，并在群里通知前端；前端不得自行猜测字段 |
| 关联文档 | [需求分析报告](requirements/需求分析报告-v1.md)、[用例图](requirements/用例图-v1.md)、[功能结构图](requirements/功能结构图-v1.md) |
| 冻结标准 | 阶段①需求评审通过；每条接口的请求/响应字段与错误码已填写；三端负责人确认无异议 |

## 变更记录

| 版本 | 日期 | 修改人 | 说明 |
| --- | --- | --- | --- |
| v0.1-draft | 阶段① | 魏鸿（起草） | 补齐全部模块接口定义、错误码、枚举与数据字典；邓裕达、陈益苇确认后升为 v0.1 并冻结 |

---

## 1. 通用约定

| 项 | 约定 |
| --- | --- |
| 基础路径 | `/api` |
| 协议 | HTTPS（本地可用 HTTP） |
| 数据格式 | 请求与响应均为 `application/json; charset=utf-8`；上传接口为 `multipart/form-data` |
| 认证 | 除注册、登录、健康检查外，请求头必须携带 `Authorization: Bearer <token>` |
| 令牌 | JWT，HS256 签名，默认有效期 7200 秒；载荷含 `sub`（账号 id）、`role`、`exp`、`iat` |
| 响应体 | 统一 `{ "code": 0, "message": "ok", "data": ... }`；`code = 0` 成功，非 0 为业务错误 |
| HTTP 状态码 | `401` 未登录 / 令牌失效；`403` 已登录但角色无权。**其余业务错误统一 HTTP 200 + 非 0 `code`**，前端只按 `code` 分支 |
| 分页 | 请求 `page`（从 1 开始，默认 1）、`size`（默认 10，最大 50）；响应的 `data` 为 `{ "total": 0, "page": 1, "size": 10, "list": [] }` |
| 时间 | ISO 8601 字符串，不带时区，服务器时区 `Asia/Shanghai`，如 `2026-11-09T15:30:00` |
| 数值 | 积分、库存、数量为整数；重量为两位小数，单位 kg（字符串或数字均可，前端按数字提交，保留 2 位） |
| 字段命名 | 一律小驼峰 `camelCase`；枚举值一律大写下划线分隔的英文单词 |
| 空值 | 未赋值字段返回 `null`，不返回空字符串占位；列表为空返回 `[]` 而非 `null` |
| 幂等 | 兑换接口必须带 `idempotencyKey`（UUID，前端生成）；同一用户同一 key 24 小时内只成交一笔 |
| 未知字段 | 服务端忽略请求体中的未知字段，避免版本错配导致 500 |

### 1.1 统一响应示例

```json
// 成功
{ "code": 0, "message": "ok", "data": { "orderId": 1001, "redeemCode": "A7K2M9QX" } }

// 业务失败（HTTP 200）
{ "code": 3001, "message": "积分不足，还差 20 积分", "data": null }

// 未登录（HTTP 401）
{ "code": 1002, "message": "登录已过期，请重新登录", "data": null }

// 越权（HTTP 403）
{ "code": 1003, "message": "当前角色无权访问该接口", "data": null }
```

### 1.2 分页响应结构

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "total": 128,
    "page": 1,
    "size": 10,
    "list": []
  }
}
```

---

## 2. 错误码

| code | 含义 | 出现场景 | HTTP |
| --- | --- | --- | --- |
| 0 | 成功 | — | 200 |
| 1001 | 参数校验失败 | 必填缺失、格式错误、长度/范围越界 | 200 |
| 1002 | 未登录或令牌失效 | 无令牌、令牌过期、签名错误 | 401 |
| 1003 | 无权限 | 角色不匹配、越权访问他人数据 | 403 |
| 1004 | 请求过于频繁 | 限流触发（可选实现） | 200 |
| 1005 | 资源不存在 | id 不存在或已删除 | 200 |
| 1006 | 当前状态不允许该操作 | 重复核销、已撤销再撤销、无订单商品欲删除 | 200 |
| 2001 | 账号或密码错误 | 登录失败（不区分账号不存在） | 200 |
| 2002 | 账号已被禁用或未通过审核 | 登录、令牌使用中账号被禁用 | 200 |
| 2003 | 手机号已注册 | 用户/回收员注册 | 200 |
| 2004 | 回收员申请未通过审核 | 回收员登录 `PENDING`/`REJECTED` | 200 |
| 2005 | 原密码错误 | 修改密码 | 200 |
| 2006 | 新密码不符合策略 | 8–20 位且含字母与数字 | 200 |
| 2007 | 临时密码已过期 | 管理员重置后超时使用 | 200 |
| 3001 | 积分余额不足 | 兑换、撤销扣回、管理员负向调整 | 200 |
| 3002 | 商品库存不足 | 兑换、并发失败 | 200 |
| 3003 | 商品已下架 | 兑换 | 200 |
| 3005 | 核销码无效或不可核销 | 不存在、已核销、已取消、已过期 | 200 |
| 3006 | 投递流水不存在 | 查不存在或不属于本人的流水 | 200 |
| 3007 | 已超过撤销时间窗口 | 回收员自助撤销超过 5 分钟 | 200 |
| 3008 | 该流水已撤销 | 重复撤销 | 200 |
| 3009 | 类别已停用 | 投递登记时选到停用类别 | 200 |
| 3010 | 重量超出允许范围 | 不在 0.01–100 kg 或小数位超 2 位 | 200 |
| 3011 | 操作会导致余额为负 | 管理员负向调整超过余额 | 200 |
| 3012 | 订单状态不允许该操作 | 对非 `PENDING` 订单取消 | 200 |
| 3013 | 兑换数量非法 | 小于 1 或大于 `min(库存,5)` | 200 |
| 3014 | 幂等键缺失 | 兑换请求未带 `idempotencyKey` | 200 |
| 4001 | 文件类型不支持 | 上传非 jpg/png/webp | 200 |
| 4002 | 文件超过大小限制 | 超过 2 MB | 200 |
| 5001 | 系统内部错误 | 未捕获异常、数据库错误 | 200 |

> 说明：`2001` 故意不区分「账号不存在」与「密码错误」，防止手机号被枚举。

---

## 3. 枚举

| 枚举 | 取值 | 使用位置 |
| --- | --- | --- |
| 角色 `role` | `USER`、`RECYCLER`、`ADMIN` | JWT 载荷、权限判断 |
| 用户状态 `userStatus` | `ACTIVE`（正常）、`DISABLED`（禁用） | 账号管理 |
| 回收员状态 `recyclerStatus` | `PENDING`（待审核）、`APPROVED`（已通过）、`REJECTED`（已驳回）、`DISABLED`（已禁用） | 审核、登录校验 |
| 流水状态 `deliveryStatus` | `NORMAL`（正常）、`REVOKED`（已撤销） | 投递流水 |
| 订单状态 `orderStatus` | `PENDING`（待核销）、`COMPLETED`（已核销）、`CANCELLED`（已取消） | 兑换订单 |
| 积分明细类型 `pointsType` | `INCOME`（收入）、`EXPENSE`（支出）、`REVOKE`（撤销扣回）、`ADJUST`（人工调整）、`REFUND`（退回） | 积分明细 |
| 积分业务来源 `bizType` | `DELIVERY`（投递）、`EXCHANGE`（兑换）、`REVOKE`（撤销）、`ADMIN_ADJUST`（管理员调整）、`REFUND`（订单取消退回）、`REWARD`（系统奖励） | 积分明细 |
| 商品状态 `productStatus` | `ON`（上架）、`OFF`（下架） | 商品 |
| 类别/站点状态 `enableStatus` | `ENABLED`、`DISABLED` | 类别、站点 |
| 公告类型 `announcementType` | `NOTICE`（系统公告）、`KNOWLEDGE`（环保科普） | 公告 |
| 公告状态 `announcementStatus` | `PUBLISHED`（已发布）、`OFFLINE`（已下线） | 公告 |
| 申诉对象类型 `targetType` | `DELIVERY`、`EXCHANGE` | 申诉 |
| 申诉状态 `appealStatus` | `PENDING`（待处理）、`ACCEPTED`（已采纳）、`REJECTED`（已驳回） | 申诉 |
| 上传场景 `scene` | `PRODUCT`（商品图）、`AVATAR`（头像）、`ANNOUNCEMENT`（公告图） | 文件上传 |
| 库存调整模式 `stockMode` | `SET`（设为指定值）、`INCR`（增量，可为负） | 库存调整 |

---

## 4. 角色与数据范围

| 角色 | 可访问接口范围 | 数据范围 |
| --- | --- | --- |
| `USER` | `/api/auth/*`（自身）、`/api/users/me/*`、`/api/products`（只读）、`/api/exchanges`（本人）、`/api/categories`、`/api/announcements`、`/api/files` | 仅本人 |
| `RECYCLER` | `/api/auth/*`（自身）、`/api/users/lookup`、`/api/deliveries`（登记/撤销/详情）、`/api/recyclers/me/*`、`/api/exchanges/redeem`、`/api/categories`（只读）、`/api/sites`（只读）、`/api/announcements`、`/api/files` | 本人经手数据 |
| `ADMIN` | `/api/admin/*` 全部、`/api/auth/*`（自身）、`/api/categories`、`/api/announcements`、`/api/files` | 全量（含统计、审计） |
| 匿名 | `/api/auth/users/register`、`/api/auth/recyclers/register`、三个登录接口、`/api/health` | 无 |

**禁止**：`ADMIN` 调 `POST /api/deliveries`、`POST /api/exchanges/redeem`（职责分离，直接 403）。

---

## 5. 接口总览

| 编号 | 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- | --- |
| API-AUTH-01 | POST | `/api/auth/users/register` | 匿名 | 用户注册 |
| API-AUTH-02 | POST | `/api/auth/users/login` | 匿名 | 用户登录 |
| API-AUTH-03 | POST | `/api/auth/recyclers/login` | 匿名 | 回收员登录 |
| API-AUTH-04 | POST | `/api/auth/admins/login` | 匿名 | 管理员登录 |
| API-AUTH-05 | POST | `/api/auth/logout` | 全部 | 登出 |
| API-AUTH-06 | PUT | `/api/auth/password` | 全部 | 修改本人密码 |
| API-AUTH-07 | POST | `/api/auth/recyclers/register` | 匿名 | 回收员注册申请 |
| API-USER-01 | GET | `/api/users/me` | USER | 查询本人资料 |
| API-USER-02 | PUT | `/api/users/me` | USER | 修改本人资料 |
| API-USER-03 | GET | `/api/users/me/qrcode` | USER | 获取身份二维码 |
| API-USER-04 | GET | `/api/users/me/points` | USER | 积分余额汇总 |
| API-USER-05 | GET | `/api/users/me/points/logs` | USER | 积分明细分页 |
| API-USER-06 | GET | `/api/users/me/deliveries` | USER | 本人投递记录 |
| API-USER-07 | GET | `/api/users/me/exchanges` | USER | 本人兑换记录 |
| API-USER-08 | GET | `/api/users/me/exchanges/{orderId}` | USER | 兑换详情与核销码 |
| API-USER-09 | POST | `/api/users/me/appeals` | USER | 提交申诉 |
| API-USER-10 | GET | `/api/users/me/appeals` | USER | 本人申诉列表 |
| API-DLV-01 | POST | `/api/deliveries` | RECYCLER | 投递登记并发放积分 |
| API-DLV-02 | GET | `/api/users/lookup` | RECYCLER | 识别用户（手机号/编号/二维码） |
| API-DLV-03 | GET | `/api/recyclers/me/deliveries` | RECYCLER | 本人经手流水 |
| API-DLV-04 | POST | `/api/deliveries/{recordId}/revoke` | RECYCLER | 5 分钟内自助撤销 |
| API-DLV-05 | GET | `/api/admin/deliveries` | ADMIN | 全量流水审计 |
| API-DLV-06 | POST | `/api/admin/deliveries/{recordId}/revoke` | ADMIN | 授权撤销 |
| API-DLV-07 | GET | `/api/deliveries/{recordId}` | RECYCLER/ADMIN | 流水详情 |
| API-EXC-01 | POST | `/api/exchanges` | USER | 积分兑换下单 |
| API-EXC-02 | GET | `/api/exchanges/{orderId}` | USER/ADMIN | 订单详情 |
| API-EXC-03 | POST | `/api/exchanges/redeem` | RECYCLER | 兑换核销 |
| API-EXC-04 | GET | `/api/recyclers/me/redemptions` | RECYCLER | 本人核销记录 |
| API-EXC-05 | GET | `/api/admin/exchanges` | ADMIN | 订单列表 |
| API-EXC-06 | POST | `/api/admin/exchanges/{orderId}/cancel` | ADMIN | 取消订单并退分 |
| API-EXC-07 | GET | `/api/exchanges/lookup` | RECYCLER | 核销前预览订单 |
| API-PRD-01 | GET | `/api/products` | USER/ADMIN | 商品列表 |
| API-PRD-02 | GET | `/api/products/{productId}` | USER/ADMIN | 商品详情 |
| API-PRD-03 | POST | `/api/admin/products` | ADMIN | 新增商品 |
| API-PRD-04 | PUT | `/api/admin/products/{productId}` | ADMIN | 编辑商品 |
| API-PRD-05 | PATCH | `/api/admin/products/{productId}/status` | ADMIN | 上下架 |
| API-PRD-06 | PATCH | `/api/admin/products/{productId}/stock` | ADMIN | 调整库存 |
| API-PRD-07 | DELETE | `/api/admin/products/{productId}` | ADMIN | 删除商品 |
| API-CAT-01 | GET | `/api/categories` | 全部登录 | 类别与当前规则 |
| API-CAT-02 | POST | `/api/admin/categories` | ADMIN | 新增类别 |
| API-CAT-03 | PUT | `/api/admin/categories/{categoryId}` | ADMIN | 编辑类别 |
| API-CAT-04 | DELETE | `/api/admin/categories/{categoryId}` | ADMIN | 删除/停用类别 |
| API-CAT-05 | PATCH | `/api/admin/categories/{categoryId}/status` | ADMIN | 启用/停用类别 |
| API-CAT-06 | PUT | `/api/admin/categories/{categoryId}/rule` | ADMIN | 设置积分规则 |
| API-CAT-07 | GET | `/api/admin/categories/{categoryId}/rules` | ADMIN | 规则历史 |
| API-SITE-01 | GET | `/api/sites` | 全部登录 | 站点列表 |
| API-SITE-02 | POST | `/api/admin/sites` | ADMIN | 新增站点 |
| API-SITE-03 | PUT | `/api/admin/sites/{siteId}` | ADMIN | 编辑站点 |
| API-SITE-04 | PATCH | `/api/admin/sites/{siteId}/status` | ADMIN | 启用/停用站点 |
| API-ACC-01 | GET | `/api/admin/accounts/users` | ADMIN | 用户列表 |
| API-ACC-02 | PATCH | `/api/admin/accounts/users/{userId}/status` | ADMIN | 禁用/启用用户 |
| API-ACC-03 | POST | `/api/admin/accounts/users/{userId}/password/reset` | ADMIN | 重置用户密码 |
| API-ACC-04 | GET | `/api/admin/accounts/recyclers` | ADMIN | 回收员列表 |
| API-ACC-05 | POST | `/api/admin/accounts/recyclers/{recyclerId}/approve` | ADMIN | 通过审核 |
| API-ACC-06 | POST | `/api/admin/accounts/recyclers/{recyclerId}/reject` | ADMIN | 驳回申请 |
| API-ACC-07 | PATCH | `/api/admin/accounts/recyclers/{recyclerId}/status` | ADMIN | 禁用/启用回收员 |
| API-ACC-08 | POST | `/api/admin/points/adjust` | ADMIN | 人工调整积分 |
| API-ACC-09 | GET | `/api/admin/appeals` | ADMIN | 申诉列表 |
| API-ACC-10 | POST | `/api/admin/appeals/{appealId}/handle` | ADMIN | 处理申诉 |
| API-ACC-11 | POST | `/api/admin/accounts/recyclers/{recyclerId}/password/reset` | ADMIN | 重置回收员密码 |
| API-STA-01 | GET | `/api/admin/stats/overview` | ADMIN | 总量看板 |
| API-STA-02 | GET | `/api/admin/stats/categories` | ADMIN | 分类占比 |
| API-STA-03 | GET | `/api/admin/stats/trend` | ADMIN | 趋势 |
| API-STA-04 | GET | `/api/admin/stats/top` | ADMIN | TOP 排行 |
| API-STA-05 | GET | `/api/recyclers/me/workbench` | RECYCLER | 个人工作台 |
| API-FIL-01 | POST | `/api/files` | 全部登录 | 文件上传 |
| API-ANN-01 | GET | `/api/announcements` | 全部登录 | 公告列表 |
| API-ANN-02 | GET | `/api/announcements/{announcementId}` | 全部登录 | 公告详情 |
| API-ANN-03 | POST | `/api/admin/announcements` | ADMIN | 新增公告 |
| API-ANN-04 | PUT | `/api/admin/announcements/{announcementId}` | ADMIN | 编辑公告 |
| API-ANN-05 | PATCH | `/api/admin/announcements/{announcementId}/status` | ADMIN | 发布/下线 |
| API-ANN-06 | DELETE | `/api/admin/announcements/{announcementId}` | ADMIN | 删除公告 |
| API-SYS-01 | GET | `/api/health` | 匿名 | 健康检查 |

---

## 6. 认证模块（AUTH）

### API-AUTH-01 用户注册

- `POST /api/auth/users/register` ｜ 匿名 ｜ 成功即登录

**请求体**

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| phone | string | 是 | 11 位手机号 |
| password | string | 是 | 8–20 位，含字母与数字 |
| nickname | string | 是 | 1–20 字符 |
| address | string | 否 | 收货地址备注，≤100 字符 |

**响应 data**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| userId | long | 用户 id |
| userCode | string | 6 位数字用户编号，供口头报给回收员 |
| token | string | JWT |
| tokenType | string | 固定 `Bearer` |
| expiresIn | int | 有效期秒数，默认 7200 |

**错误码**：`1001` 参数非法、`2003` 手机号已注册

```json
{ "phone": "13800001234", "password": "abc12345", "nickname": "小魏", "address": "3 号楼 502" }
```

### API-AUTH-02 用户登录

- `POST /api/auth/users/login` ｜ 匿名

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| phone | string | 是 | 手机号 |
| password | string | 是 | 密码 |

**响应 data**：`token`、`tokenType`、`expiresIn`、`user{ userId, nickname, avatarUrl, pointsBalance }`

**错误码**：`2001` 账号或密码错误、`2002` 账号被禁用

### API-AUTH-03 回收员登录

- `POST /api/auth/recyclers/login` ｜ 匿名

请求同上（`phone` + `password`）。**响应 data**：`token`、`tokenType`、`expiresIn`、`recycler{ recyclerId, name, siteId, siteName, status }`

**错误码**：`2001`、`2002` 已禁用、`2004` 审核未通过（`message` 中说明待审/驳回原因）

### API-AUTH-04 管理员登录

- `POST /api/auth/admins/login` ｜ 匿名

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | string | 是 | 后台用户名 |
| password | string | 是 | 密码 |

**响应 data**：`token`、`tokenType`、`expiresIn`、`admin{ adminId, username }`

**错误码**：`2001`、`2002`

### API-AUTH-05 登出

- `POST /api/auth/logout` ｜ USER/RECYCLER/ADMIN

**响应 data**：`null`。说明：JWT 无状态，前端清空本地令牌即可；`v0.2` 若引入令牌黑名单（Redis）则服务端同时失效该令牌。

### API-AUTH-06 修改本人密码

- `PUT /api/auth/password` ｜ USER/RECYCLER/ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| oldPassword | string | 是 | 原密码 |
| newPassword | string | 是 | 新密码，8–20 位含字母与数字，且不得与原密码相同 |

**响应 data**：`null`；成功后**令牌立即失效**，前端跳登录页。

**错误码**：`2005` 原密码错误、`2006` 新密码不符合策略

### API-AUTH-07 回收员注册申请

- `POST /api/auth/recyclers/register` ｜ 匿名

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| name | string | 是 | 真实姓名，2–20 字符 |
| phone | string | 是 | 手机号，全局唯一 |
| password | string | 是 | 密码策略同上 |
| employeeNo | string | 是 | 工号，4–20 位 |
| siteId | long | 否 | 意向站点 |
| reason | string | 否 | 申请说明，≤200 字 |

**响应 data**：`recyclerId`、`status`（固定 `PENDING`）

**错误码**：`1001`、`2003`。说明：申请后不能登录（返回 2004），需管理员审核（API-ACC-05/06）。

---

## 7. 用户资料与积分（USER）

### API-USER-01 查询本人资料

- `GET /api/users/me` ｜ USER

**响应 data**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| userId | long | 用户 id |
| userCode | string | 6 位用户编号 |
| phone | string | 完整手机号（仅本人可见） |
| nickname | string | 昵称 |
| avatarUrl | string | 头像 URL，可为空 |
| address | string | 地址备注 |
| pointsBalance | int | 当前积分余额 |
| status | string | `ACTIVE` / `DISABLED` |
| createdAt | string | 注册时间 |

### API-USER-02 修改本人资料

- `PUT /api/users/me` ｜ USER

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| nickname | string | 否 | 1–20 字符 |
| avatarUrl | string | 否 | 由 API-FIL-01 返回的 URL |
| address | string | 否 | ≤100 字符 |

**响应 data**：更新后的资料对象（同 API-USER-01）。**不可修改**：`phone`、`pointsBalance`、`status`、`role`（传了也忽略）。

**错误码**：`1001`

### API-USER-03 获取身份二维码

- `GET /api/users/me/qrcode` ｜ USER

**响应 data**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| userCode | string | 6 位数字编号，供手输 |
| qrContent | string | 二维码内容，形如 `NUAA-RECYCLE:U:<token>`，回收员扫码后原样回传 |
| expiresIn | int | 有效秒数，默认 600 |
| expiresAt | string | 过期时间 |

**错误码**：无（令牌已登录即返回）。说明：`qrContent` 由服务端用专用密钥签名，过期或签名错误在 API-DLV-02 中返回 `1001`。

### API-USER-04 积分余额汇总

- `GET /api/users/me/points` ｜ USER

**响应 data**：`balance`（当前余额）、`totalIncome`（累计收入）、`totalExpense`（累计支出）、`totalRevoked`（累计撤销扣回）

### API-USER-05 积分明细分页

- `GET /api/users/me/points/logs?page=1&size=10&type=INCOME` ｜ USER

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| page / size | query | int | 否 | 分页 |
| type | query | string | 否 | `pointsType` 枚举，不传为全部 |
| from / to | query | string | 否 | 时间范围（ISO 8601） |

**响应 list 元素**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| logId | long | 明细 id |
| type | string | `INCOME`/`EXPENSE`/`REVOKE`/`ADJUST`/`REFUND` |
| change | int | 变动值，正数收入、负数支出 |
| balanceAfter | int | 变动后余额 |
| bizType | string | `DELIVERY`/`EXCHANGE`/`REVOKE`/`ADMIN_ADJUST`/`REFUND`/`REWARD` |
| bizNo | string | 来源单号（投递流水号或订单号） |
| remark | string | 备注（撤销原因、调整原因等） |
| createdAt | string | 变动时间 |

### API-USER-06 本人投递记录

- `GET /api/users/me/deliveries?page=1&size=10&categoryId=&from=&to=` ｜ USER

**响应 list 元素**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| recordId | long | 流水 id |
| recordNo | string | 流水号，如 `D20261109001` |
| categoryId / categoryName | long / string | 垃圾类别 |
| weight | number | 重量 kg，两位小数 |
| points | int | 获得积分 |
| recyclerName | string | 回收员姓名 |
| siteName | string | 站点名称，可空 |
| status | string | `NORMAL` / `REVOKED` |
| revokeReason | string | 撤销原因，仅 `REVOKED` 有值 |
| revokedAt | string | 撤销时间，可空 |
| createdAt | string | 投递时间 |

### API-USER-07 本人兑换记录

- `GET /api/users/me/exchanges?page=1&size=10&status=PENDING` ｜ USER

**响应 list 元素**：`orderId`、`orderNo`、`productId`、`productName`、`productCoverUrl`、`quantity`、`pointsCost`、`status`、`redeemCode`（列表页也返回，便于快速出示）、`createdAt`、`verifiedAt`、`cancelReason`

### API-USER-08 兑换详情与核销码

- `GET /api/users/me/exchanges/{orderId}` ｜ USER（仅本人）

**响应 data**：上述字段 + `redeemCodeExpireAt`（核销码过期时间）、`siteName`（核销站点）、`verifierName`（核销人姓名，可空）

**错误码**：`1005` 订单不存在、`1003` 非本人订单

### API-USER-09 提交申诉

- `POST /api/users/me/appeals` ｜ USER

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| targetType | string | 是 | `DELIVERY` / `EXCHANGE` |
| targetId | long | 是 | 流水 id 或订单 id，必须属于本人 |
| description | string | 是 | 问题描述，10–500 字 |

**响应 data**：`appealId`、`status`（`PENDING`）、`createdAt`

**错误码**：`1001`、`1005`、`1003`

### API-USER-10 本人申诉列表

- `GET /api/users/me/appeals?page=1&size=10` ｜ USER

**响应 list 元素**：`appealId`、`targetType`、`targetId`、`targetNo`、`description`、`status`、`conclusion`（处理结论，可空）、`handledAt`、`createdAt`

---

## 8. 投递与撤销（DELIVERY）

### API-DLV-01 投递登记并发放积分

- `POST /api/deliveries` ｜ **RECYCLER**（ADMIN 调用直接 403）

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| userId | long | 是 | 通过 API-DLV-02 识别得到的用户 id |
| categoryId | long | 是 | 垃圾类别 id，必须处于启用状态 |
| weight | number | 是 | 0.01–100，最多两位小数 |
| remark | string | 否 | 备注，≤100 字 |

**响应 data**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| recordId | long | 流水 id |
| recordNo | string | 流水号 |
| points | int | 本次获得积分（`round(weight × 每公斤积分)`，最低 1 分） |
| weight | number | 回显重量 |
| categoryName | string | 类别名称 |
| balanceAfter | int | 用户变动后余额 |
| createdAt | string | 投递时间 |
| revocableBefore | string | 可自助撤销的截止时间（`createdAt + 5 分钟`），前端据此置灰按钮 |

**错误码**：`3009` 类别已停用、`3010` 重量越界、`1005` 用户或类别不存在、`1003` 非回收员

说明：写流水、加余额、写明细必须同一事务；返回的 `points` 由服务端计算，请求体若含 `points` 一律忽略（BR-01）。

### API-DLV-02 识别用户

- `GET /api/users/lookup?keyword=13800001234` ｜ RECYCLER

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| keyword | query | string | 二选一 | 手机号（11 位）或 6 位用户编号 |
| qrContent | query | string | 二选一 | 扫码得到的 `qrContent` 原文 |

**响应 data**：`userId`、`userCode`、`nickname`、`phoneMasked`（`138****1234`）、`avatarUrl`、`pointsBalance`、`status`

**错误码**：`1001` 关键字非法或二维码失效/签名错误、`1005` 用户不存在、`2002` 用户已被禁用

说明：**不返回**地址、完整手机号等隐私字段。

### API-DLV-03 本人经手流水

- `GET /api/recyclers/me/deliveries?page=1&size=10&from=&to=&categoryId=` ｜ RECYCLER

**响应 list 元素**：`recordId`、`recordNo`、`userNickname`、`userPhoneMasked`、`categoryName`、`weight`、`points`、`status`、`revokeReason`、`revokedAt`、`createdAt`、`revocable`（布尔，服务端按 5 分钟窗口计算）

说明：**服务端强制附加 `recycler_id = 当前登录人`**，请求体/参数中的 `recyclerId` 一律忽略（BR-05）。

### API-DLV-04 自助撤销投递流水

- `POST /api/deliveries/{recordId}/revoke` ｜ RECYCLER

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| reason | string | 是 | 撤销原因，10–100 字 |

**响应 data**：`recordId`、`status`（`REVOKED`）、`refundPoints`（扣回积分数，正数表示从用户扣除）、`balanceAfter`、`revokedAt`

**错误码**：`3006` 流水不存在或非本人、`3008` 已撤销、`3007` 超过 5 分钟、`3001` 用户余额不足以扣回

### API-DLV-05 全量流水审计

- `GET /api/admin/deliveries?page=1&size=20&recyclerId=&categoryId=&from=&to=&status=&abnormalOnly=false` ｜ ADMIN

**响应 data**：分页对象 + `summary{ totalCount, totalWeight, totalPoints, revokedCount }`

**list 元素**：`recordId`、`recordNo`、`userId`、`userNickname`、`userPhoneMasked`、`recyclerId`、`recyclerName`、`siteName`、`categoryName`、`weight`、`points`、`status`、`revokeReason`、`revokedBy`、`revokedAt`、`createdAt`、`abnormalFlags`（数组，如 `["OVER_WEIGHT","HIGH_FREQUENCY"]`）

说明：`abnormalOnly=true` 时只返回命中异常规则的记录；异常规则建议：单笔重量 > 50 kg、同一用户 1 小时内 ≥ 5 笔、同一回收员 10 分钟内 ≥ 10 笔（阈值可配置，`v0.1` 写死）。

### API-DLV-06 授权撤销

- `POST /api/admin/deliveries/{recordId}/revoke` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| reason | string | 是 | 授权撤销原因，10–200 字 |

**响应 data**：同 API-DLV-04，另加 `revokedBy`（管理员用户名）

**错误码**：`3006`、`3008`、`3001`（余额不足时扣至 0 并返回 `deficit` 字段说明差额）

### API-DLV-07 流水详情

- `GET /api/deliveries/{recordId}` ｜ RECYCLER（限本人经手）/ ADMIN

**响应 data**：同 API-DLV-05 的 list 元素单条

**错误码**：`1005`、`1003`

---

## 9. 商城与兑换（EXCHANGE）

### API-EXC-01 积分兑换下单

- `POST /api/exchanges` ｜ USER

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| productId | long | 是 | 商品 id |
| quantity | int | 是 | 1–5，且不超过库存 |
| idempotencyKey | string | 是 | UUID，前端每次点击生成一次，重试沿用同一个 |

**响应 data**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| orderId | long | 订单 id |
| orderNo | string | 订单号，如 `E202611090001` |
| redeemCode | string | 8 位核销码（大写字母+数字） |
| redeemCodeExpireAt | string | 核销码过期时间（+30 天） |
| productName | string | 商品名称 |
| quantity | int | 数量 |
| pointsCost | int | 消耗积分 |
| balanceAfter | int | 剩余积分 |
| status | string | `PENDING` |

**错误码**：`3001` 积分不足（`message` 含差额）、`3002` 库存不足、`3003` 已下架、`3013` 数量非法、`3014` 缺少幂等键

说明：扣积分与扣库存必须原子（乐观锁版本号或 Redis 锁）；并发失败返回 `3002`，不得出现「扣了积分没减库存」。

### API-EXC-02 订单详情

- `GET /api/exchanges/{orderId}` ｜ USER（本人）/ ADMIN

**响应 data**：`orderId`、`orderNo`、`userId`、`userNickname`、`productId`、`productName`、`productCoverUrl`、`quantity`、`pointsCost`、`status`、`redeemCode`（本人或管理员可见）、`redeemCodeExpireAt`、`siteName`、`verifierName`、`verifiedAt`、`cancelReason`、`createdAt`

**错误码**：`1005`、`1003`

### API-EXC-03 兑换核销

- `POST /api/exchanges/redeem` ｜ **RECYCLER**（ADMIN 调用 403）

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| redeemCode | string | 是 | 8 位核销码，忽略大小写与首尾空格 |

**响应 data**：`orderId`、`orderNo`、`productName`、`quantity`、`userNickname`、`status`（`COMPLETED`）、`verifiedAt`、`verifierName`

**错误码**：`3005` 核销码无效/已核销/已取消/已过期

说明：核销前建议先调 API-EXC-07 预览并二次确认；核销操作幂等要求：同一码重复提交返回 `3005`，不产生重复完成记录。

### API-EXC-04 本人核销记录

- `GET /api/recyclers/me/redemptions?page=1&size=10&from=&to=` ｜ RECYCLER

**响应 list 元素**：`orderId`、`orderNo`、`userNickname`、`productName`、`quantity`、`pointsCost`、`verifiedAt`

### API-EXC-05 订单列表

- `GET /api/admin/exchanges?page=1&size=20&status=&userId=&orderNo=` ｜ ADMIN

**响应 list 元素**：`orderId`、`orderNo`、`userId`、`userNickname`、`productName`、`quantity`、`pointsCost`、`status`、`redeemCode`、`verifiedAt`、`cancelReason`、`createdAt`

### API-EXC-06 取消订单

- `POST /api/admin/exchanges/{orderId}/cancel` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| reason | string | 是 | 取消原因，10–200 字（如「缺货」） |

**响应 data**：`orderId`、`status`（`CANCELLED`）、`refundPoints`、`restoredStock`、`balanceAfter`

**错误码**：`3012` 仅 `PENDING` 可取消、`1005`

说明：退分与恢复库存必须原子，并写 `REFUND` 明细（与原订单号关联）。

### API-EXC-07 核销前预览

- `GET /api/exchanges/lookup?redeemCode=A7K2M9QX` ｜ RECYCLER

**响应 data**：`orderId`、`orderNo`、`userNickname`、`userPhoneMasked`、`productName`、`productCoverUrl`、`quantity`、`pointsCost`、`status`、`createdAt`、`redeemCodeExpireAt`

**错误码**：`3005`

---

## 10. 商品（PRODUCT）

### API-PRD-01 商品列表

- `GET /api/products?page=1&size=10&keyword=&sort=points_asc&onlyAvailable=false` ｜ USER / ADMIN

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| keyword | query | string | 否 | 名称模糊搜索，≤50 字 |
| sort | query | string | 否 | `points_asc`、`points_desc`、`created_desc`（默认） |
| onlyAvailable | query | boolean | 否 | `true` 时只返回库存 > 0 的商品 |

**响应 list 元素**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| productId | long | 商品 id |
| name | string | 名称 |
| coverUrl | string | 封面 |
| pointsPrice | int | 所需积分 |
| stock | int | 剩余库存 |
| status | string | `ON` / `OFF`（USER 侧只返回 `ON`） |
| description | string | 简介，列表页截断展示 |

### API-PRD-02 商品详情

- `GET /api/products/{productId}` ｜ USER / ADMIN

**响应 data**：列表字段 + `spec`（规格说明）、`exchangeNotice`（兑换须知）、`salesCount`（已完成订单数量）、`createdAt`、`updatedAt`

**错误码**：`1005`

### API-PRD-03 新增商品

- `POST /api/admin/products` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| name | string | 是 | 1–50 字 |
| description | string | 否 | ≤500 字 |
| coverUrl | string | 否 | API-FIL-01 返回的 URL |
| pointsPrice | int | 是 | 1–100000 |
| stock | int | 是 | 0–99999 |
| spec | string | 否 | ≤100 字 |
| exchangeNotice | string | 否 | ≤200 字 |

**响应 data**：`productId`

**错误码**：`1001`

### API-PRD-04 编辑商品

- `PUT /api/admin/products/{productId}` ｜ ADMIN

请求字段同 API-PRD-03（均可选，传哪个改哪个，`null` 不覆盖）。**不允许改库存**（走 API-PRD-06），避免与并发扣减冲突。

**响应 data**：更新后的商品对象

### API-PRD-05 上下架

- `PATCH /api/admin/products/{productId}/status` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| status | string | 是 | `ON` / `OFF` |

**响应 data**：`productId`、`status`。说明：下架不影响已生成订单，`PENDING` 订单仍可核销。

### API-PRD-06 调整库存

- `PATCH /api/admin/products/{productId}/stock` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| mode | string | 是 | `SET`（设为） / `INCR`（增减，`value` 可为负） |
| value | int | 是 | `SET` 时 0–99999；`INCR` 时 -99999–99999 |
| reason | string | 是 | 调整原因，≤100 字（入库/盘点/损耗） |

**响应 data**：`productId`、`stockBefore`、`stock`、`adjustId`

**错误码**：`1001`、`1005`、`3011`（`INCR` 后为负）

### API-PRD-07 删除商品

- `DELETE /api/admin/products/{productId}` ｜ ADMIN

**响应 data**：`null`。说明：仅当该商品**没有关联订单**时可物理删除；否则返回 `1006`，请改为下架（BR-12）。

---

## 11. 类别、积分规则与站点（CATEGORY / SITE）

### API-CAT-01 类别与当前规则

- `GET /api/categories?enabledOnly=true` ｜ 全部登录角色（回收员只读用于解释规则）

**响应 list 元素**：`categoryId`、`name`、`code`、`unit`（如 `kg`）、`iconUrl`、`sort`、`status`、`pointsPerKg`（当前生效系数）、`ruleEffectiveFrom`

### API-CAT-02 新增类别

- `POST /api/admin/categories` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| name | string | 是 | 1–20 字，如「废纸」 |
| code | string | 是 | 唯一编码，大写字母与下划线，如 `PAPER` |
| unit | string | 否 | 默认 `kg` |
| iconUrl | string | 否 | 图标 URL |
| sort | int | 否 | 排序值，越小越前，默认 0 |
| pointsPerKg | int | 是 | 初始积分系数，≥1 |

**响应 data**：`categoryId`。**错误码**：`1001`（编码重复返回 1001，`message` 说明）

### API-CAT-03 编辑类别

- `PUT /api/admin/categories/{categoryId}` ｜ ADMIN

请求字段：`name`、`unit`、`iconUrl`、`sort`（`code` 创建后不可改，避免历史数据口径漂移）。

**响应 data**：更新后的类别对象

### API-CAT-04 删除类别

- `DELETE /api/admin/categories/{categoryId}` ｜ ADMIN

**响应 data**：`null`。说明：无流水引用时物理删除；有引用则返回 `1006`，请改为停用 `API-CAT-05`。

### API-CAT-05 启用/停用类别

- `PATCH /api/admin/categories/{categoryId}/status` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| status | string | 是 | `ENABLED` / `DISABLED` |

**响应 data**：`categoryId`、`status`。说明：停用后回收员端不再可选（`3009`），历史流水不变。

### API-CAT-06 设置积分规则

- `PUT /api/admin/categories/{categoryId}/rule` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| pointsPerKg | int | 是 | 每公斤积分，1–1000 |
| effectiveFrom | string | 否 | ISO 8601，默认立即生效；不得早于当前生效规则的生效时间 |

**响应 data**：`ruleId`、`categoryId`、`pointsPerKg`、`effectiveFrom`

**错误码**：`1001`、`1005`。说明：**仅供此后新流水使用**，历史流水积分不重算（BR-02/BR-14），前端需在确认弹窗中提示这一点。

### API-CAT-07 规则历史

- `GET /api/admin/categories/{categoryId}/rules?page=1&size=20` ｜ ADMIN

**响应 list 元素**：`ruleId`、`pointsPerKg`、`effectiveFrom`、`createdBy`、`createdAt`

### API-SITE-01 站点列表

- `GET /api/sites?enabledOnly=true` ｜ 全部登录角色（回收员看到自己站点，管理员看全量）

**响应 list 元素**：`siteId`、`name`、`address`、`openTime`（如 `08:00-20:00`）、`manager`、`phone`、`status`

### API-SITE-02 新增站点

- `POST /api/admin/sites` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| name | string | 是 | 1–30 字 |
| address | string | 是 | ≤100 字 |
| openTime | string | 否 | ≤50 字 |
| manager | string | 否 | ≤20 字 |
| phone | string | 否 | 联系方式 |

**响应 data**：`siteId`

### API-SITE-03 编辑站点

- `PUT /api/admin/sites/{siteId}` ｜ ADMIN；请求字段同 API-SITE-02。**响应 data**：更新后的站点对象

### API-SITE-04 启用/停用站点

- `PATCH /api/admin/sites/{siteId}/status` ｜ ADMIN；`{ "status": "ENABLED" | "DISABLED" }`。停用不影响已有回收员的归属记录。

---

## 12. 账号、审核、审计与申诉（ACCOUNT）

### API-ACC-01 用户列表

- `GET /api/admin/accounts/users?page=1&size=20&keyword=&status=` ｜ ADMIN

**响应 list 元素**：`userId`、`userCode`、`nickname`、`phoneMasked`、`pointsBalance`、`status`、`deliveryCount`、`orderCount`、`createdAt`

### API-ACC-02 禁用/启用用户

- `PATCH /api/admin/accounts/users/{userId}/status` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| status | string | 是 | `ACTIVE` / `DISABLED` |
| reason | string | 否 | 禁用原因，禁用时必填，≤200 字 |

**响应 data**：`userId`、`status`。说明：禁用后该用户所有接口返回 `2002`，已生成的 `PENDING` 订单不受影响。

### API-ACC-03 重置用户密码

- `POST /api/admin/accounts/users/{userId}/password/reset` ｜ ADMIN

**响应 data**：`tempPassword`（10 位随机，一次性展示）、`expireAt`（建议 24 小时）

说明：**不提供**直接设置用户新密码的接口（BR-06）；管理员不得知晓用户长期密码。

### API-ACC-04 回收员列表

- `GET /api/admin/accounts/recyclers?page=1&size=20&status=PENDING&keyword=` ｜ ADMIN

**响应 list 元素**：`recyclerId`、`name`、`phone`（管理端完整可见）、`employeeNo`、`siteId`、`siteName`、`status`、`reason`（申请说明）、`rejectReason`、`createdAt`、`approvedAt`

### API-ACC-05 通过审核

- `POST /api/admin/accounts/recyclers/{recyclerId}/approve` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| siteId | long | 是 | 分配站点，必须为启用状态 |

**响应 data**：`recyclerId`、`status`（`APPROVED`）、`siteId`、`siteName`

**错误码**：`1005`、`1006`（仅 `PENDING` 可审核）、`1001`

### API-ACC-06 驳回申请

- `POST /api/admin/accounts/recyclers/{recyclerId}/reject` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| reason | string | 是 | 驳回原因，10–200 字 |

**响应 data**：`recyclerId`、`status`（`REJECTED`）。被驳回后登录返回 `2004`，可重新提交申请。

### API-ACC-07 禁用/启用回收员

- `PATCH /api/admin/accounts/recyclers/{recyclerId}/status` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| status | string | 是 | `APPROVED`（启用）/ `DISABLED`（禁用） |
| reason | string | 否 | 禁用原因 |

**响应 data**：`recyclerId`、`status`

### API-ACC-08 人工调整积分

- `POST /api/admin/points/adjust` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| userId | long | 是 | 目标用户 |
| change | int | 是 | 变动值，正数为补分、负数为扣分，绝对值 ≤100000 且不为 0 |
| reason | string | 是 | 调整原因，10–200 字（必填，审计追溯用） |

**响应 data**：`logId`、`userId`、`change`、`balanceBefore`、`balanceAfter`

**错误码**：`3011` 扣至负数、`1005`

说明：写 `ADJUST` 明细并记录操作人；此为「违规积分撤销」与「申诉补偿」的唯一入口。

### API-ACC-09 申诉列表

- `GET /api/admin/appeals?page=1&size=20&status=PENDING` ｜ ADMIN

**响应 list 元素**：`appealId`、`userId`、`userNickname`、`userPhoneMasked`、`targetType`、`targetId`、`targetNo`、`description`、`status`、`conclusion`、`handledBy`、`handledAt`、`createdAt`

### API-ACC-10 处理申诉

- `POST /api/admin/appeals/{appealId}/handle` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| result | string | 是 | `ACCEPT`（采纳）/ `REJECT`（驳回） |
| conclusion | string | 是 | 处理结论，10–500 字，需写明是否补偿积分及关联明细号 |

**响应 data**：`appealId`、`status`（`ACCEPTED` / `REJECTED`）、`handledAt`

**错误码**：`1006` 已处理过、`1005`

说明：需要补偿时**另调** API-ACC-08，本接口不直接改积分，职责单一。

### API-ACC-11 重置回收员密码

- `POST /api/admin/accounts/recyclers/{recyclerId}/password/reset` ｜ ADMIN

**响应 data**：`tempPassword`、`expireAt`（同 API-ACC-03 规则）

---

## 13. 统计看板（STATS）

### API-STA-01 总量看板

- `GET /api/admin/stats/overview` ｜ ADMIN

**响应 data**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| totalWeight | number | 累计回收重量 kg（仅 `NORMAL` 流水） |
| totalPointsIssued | int | 累计发放积分（仅 `NORMAL` 流水） |
| totalPointsRedeemed | int | 累计消耗积分（仅 `COMPLETED` 订单） |
| userCount | int | 注册用户数 |
| recyclerCount | int | 已通过审核的回收员数 |
| completedOrderCount | int | 已完成订单数 |
| pendingRecyclerCount | int | 待审核回收员数 |
| pendingAppealCount | int | 待处理申诉数 |
| expiredPendingOrderCount | int | 超 30 天未核销的 `PENDING` 订单数（待处理提醒） |

### API-STA-02 分类占比

- `GET /api/admin/stats/categories?from=&to=` ｜ ADMIN

**响应 list 元素**：`categoryId`、`categoryName`、`weight`、`weightRatio`（0–1，两位小数）、`points`、`recordCount`

### API-STA-03 趋势

- `GET /api/admin/stats/trend?days=7` ｜ ADMIN（`days` ∈ {7, 30}）

**响应 list 元素**：`date`（`YYYY-MM-DD`）、`weight`、`points`、`orderCount`、`deliveryCount`

### API-STA-04 TOP 排行

- `GET /api/admin/stats/top?type=PRODUCT&limit=5` ｜ ADMIN

| 参数 | 位置 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| type | query | string | 是 | `PRODUCT`（兑换最多） / `USER`（投递最多） |
| limit | query | int | 否 | 1–20，默认 5 |

**响应 list 元素**：`id`、`name`（USER 类型返回脱敏昵称）、`value`（数量或重量）、`extra`（附加指标，如消耗积分）

### API-STA-05 回收员工作台

- `GET /api/recyclers/me/workbench` ｜ RECYCLER

**响应 data**：`todayWeight`、`todayPoints`、`todayDeliveryCount`、`todayRedeemCount`、`siteName`、`weekTrend[{ date, weight }]`、`monthRank`（本月本人在站点的排名，可空）

---

## 14. 文件上传（FILE）

### API-FIL-01 上传文件

- `POST /api/files` ｜ USER / RECYCLER / ADMIN ｜ `multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| file | file | 是 | jpg / png / webp，≤2 MB |
| scene | string | 是 | `PRODUCT` / `AVATAR` / `ANNOUNCEMENT` |

**响应 data**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| fileId | long | 文件 id |
| url | string | 可访问 URL（本地存储时为 `/uploads/2026/11/xxx.png` 形式） |
| size | int | 字节数 |
| mime | string | 如 `image/png` |

**错误码**：`4001` 类型不支持、`4002` 超过大小限制

说明：服务端需校验真实文件头而非仅扩展名；文件名随机化，禁止路径穿越。

---

## 15. 公告与科普（ANNOUNCEMENT）

### API-ANN-01 公告列表

- `GET /api/announcements?page=1&size=10&type=NOTICE` ｜ 全部登录角色

**响应 list 元素**：`announcementId`、`title`、`type`、`summary`（正文前 100 字）、`coverUrl`、`publishedAt`

说明：用户端只返回 `PUBLISHED`；管理员列表另用 `GET /api/admin/announcements`（见下）。

### API-ANN-02 公告详情

- `GET /api/announcements/{announcementId}` ｜ 全部登录角色

**响应 data**：`announcementId`、`title`、`type`、`content`（纯文本，保留换行）、`coverUrl`、`publishedAt`、`status`

**错误码**：`1005`

### API-ANN-03 新增公告

- `POST /api/admin/announcements` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| title | string | 是 | 1–50 字 |
| type | string | 是 | `NOTICE` / `KNOWLEDGE` |
| content | string | 是 | 1–5000 字，纯文本 |
| coverUrl | string | 否 | 封面图 URL |
| status | string | 否 | `PUBLISHED`（默认）/ `OFFLINE` |

**响应 data**：`announcementId`

### API-ANN-04 编辑公告

- `PUT /api/admin/announcements/{announcementId}` ｜ ADMIN；字段同 API-ANN-03（均可选）。**响应 data**：更新后的公告对象

### API-ANN-05 发布/下线

- `PATCH /api/admin/announcements/{announcementId}/status` ｜ ADMIN

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| status | string | 是 | `PUBLISHED` / `OFFLINE` |

**响应 data**：`announcementId`、`status`、`publishedAt`

### API-ANN-06 删除公告

- `DELETE /api/admin/announcements/{announcementId}` ｜ ADMIN

**响应 data**：`null`。说明：物理删除即可（公告无业务关联数据），或软删除以保留历史。

---

## 16. 系统（SYS）

### API-SYS-01 健康检查

- `GET /api/health` ｜ 匿名

**响应 data**：`status`（`UP`）、`time`、`version`

说明：供部署验证与 Nginx 探活使用，不校验令牌。

---

## 17. 数据字典

> 字段命名以数据库为准；接口返回的小驼峰由 ORM 映射。索引与约束供后端建表参考，最终以 `docs/design/数据库设计` 为准。

### 17.1 user（用户）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK, auto | 用户 id |
| user_code | char(6) | UNIQUE, NOT NULL | 6 位用户编号 |
| phone | varchar(11) | UNIQUE, NOT NULL | 手机号（登录名） |
| password_hash | varchar(100) | NOT NULL | BCrypt 哈希 |
| nickname | varchar(20) | NOT NULL | 昵称 |
| avatar_url | varchar(255) | | 头像 |
| address | varchar(100) | | 地址备注 |
| points_balance | int | NOT NULL DEFAULT 0, CHECK ≥ 0 | 积分余额 |
| status | varchar(10) | NOT NULL DEFAULT 'ACTIVE' | `ACTIVE` / `DISABLED` |
| disabled_reason | varchar(200) | | 禁用原因 |
| created_at / updated_at | datetime | NOT NULL | 时间 |

### 17.2 recycler（回收员）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK | 回收员 id |
| name | varchar(20) | NOT NULL | 姓名 |
| phone | varchar(11) | UNIQUE, NOT NULL | 登录名 |
| password_hash | varchar(100) | NOT NULL | 密码哈希 |
| employee_no | varchar(20) | NOT NULL | 工号 |
| site_id | bigint | FK → site.id | 所属站点 |
| status | varchar(10) | NOT NULL DEFAULT 'PENDING' | `PENDING`/`APPROVED`/`REJECTED`/`DISABLED` |
| apply_reason | varchar(200) | | 申请说明 |
| reject_reason | varchar(200) | | 驳回原因 |
| approved_by | bigint | FK → admin_user.id | 审核人 |
| approved_at | datetime | | 审核时间 |
| created_at / updated_at | datetime | NOT NULL | 时间 |

### 17.3 admin_user（管理员）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK | 管理员 id |
| username | varchar(30) | UNIQUE, NOT NULL | 登录名 |
| password_hash | varchar(100) | NOT NULL | 密码哈希 |
| status | varchar(10) | NOT NULL DEFAULT 'ACTIVE' | 状态 |
| created_at / updated_at | datetime | NOT NULL | 时间 |

### 17.4 site（回收站点）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK | 站点 id |
| name | varchar(30) | NOT NULL | 名称 |
| address | varchar(100) | NOT NULL | 地址 |
| open_time | varchar(50) | | 开放时间 |
| manager | varchar(20) | | 负责人 |
| phone | varchar(20) | | 联系电话 |
| status | varchar(10) | NOT NULL DEFAULT 'ENABLED' | 启用状态 |

### 17.5 recycle_category（垃圾类别）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK | 类别 id |
| name | varchar(20) | NOT NULL | 名称 |
| code | varchar(20) | UNIQUE, NOT NULL | 编码 |
| unit | varchar(10) | NOT NULL DEFAULT 'kg' | 计量单位 |
| icon_url | varchar(255) | | 图标 |
| sort | int | NOT NULL DEFAULT 0 | 排序 |
| status | varchar(10) | NOT NULL DEFAULT 'ENABLED' | 启用状态 |
| created_at / updated_at | datetime | NOT NULL | 时间 |

### 17.6 points_rule（积分规则）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK | 规则 id |
| category_id | bigint | FK, NOT NULL | 类别 |
| points_per_kg | int | NOT NULL | 每公斤积分 |
| effective_from | datetime | NOT NULL | 生效时间 |
| created_by | bigint | FK → admin_user.id | 配置人 |
| created_at | datetime | NOT NULL | 时间 |

索引：`idx_category_effective (category_id, effective_from DESC)`。

### 17.7 delivery_record（投递流水）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK | 流水 id |
| record_no | varchar(24) | UNIQUE, NOT NULL | 流水号 |
| user_id | bigint | FK, NOT NULL | 用户 |
| recycler_id | bigint | FK, NOT NULL | 录入回收员（数据隔离依据） |
| site_id | bigint | FK | 站点 |
| category_id | bigint | FK, NOT NULL | 类别 |
| rule_id | bigint | FK | 使用的规则版本（审计用） |
| weight | decimal(6,2) | NOT NULL, CHECK > 0 | 重量 kg |
| points | int | NOT NULL | 本次积分 |
| status | varchar(10) | NOT NULL DEFAULT 'NORMAL' | `NORMAL`/`REVOKED` |
| remark | varchar(100) | | 备注 |
| revoke_reason | varchar(200) | | 撤销原因 |
| revoked_by_type | varchar(10) | | `RECYCLER`/`ADMIN` |
| revoked_by_id | bigint | | 撤销人 |
| revoked_at | datetime | | 撤销时间 |
| created_at | datetime | NOT NULL | 投递时间 |

索引：`idx_user_created (user_id, created_at DESC)`、`idx_recycler_created (recycler_id, created_at DESC)`、`idx_status (status)`。

### 17.8 points_log（积分明细）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK | 明细 id |
| user_id | bigint | FK, NOT NULL | 用户 |
| type | varchar(10) | NOT NULL | 明细类型 |
| change | int | NOT NULL, ≠0 | 变动值 |
| balance_after | int | NOT NULL | 变动后余额 |
| biz_type | varchar(20) | NOT NULL | 业务来源 |
| biz_id | bigint | | 业务主键（流水/订单 id） |
| biz_no | varchar(24) | | 业务单号 |
| operator_type | varchar(10) | | `SYSTEM`/`RECYCLER`/`ADMIN` |
| operator_id | bigint | | 操作人 |
| remark | varchar(200) | | 备注/原因 |
| created_at | datetime | NOT NULL | 时间 |

索引：`idx_user_created (user_id, created_at DESC)`。

### 17.9 product（商品）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK | 商品 id |
| name | varchar(50) | NOT NULL | 名称 |
| description | varchar(500) | | 描述 |
| cover_url | varchar(255) | | 封面 |
| points_price | int | NOT NULL, CHECK ≥ 1 | 所需积分 |
| stock | int | NOT NULL DEFAULT 0, CHECK ≥ 0 | 库存 |
| sold_count | int | NOT NULL DEFAULT 0 | 已兑数量 |
| spec | varchar(100) | | 规格 |
| exchange_notice | varchar(200) | | 兑换须知 |
| status | varchar(10) | NOT NULL DEFAULT 'ON' | `ON`/`OFF` |
| version | int | NOT NULL DEFAULT 0 | **乐观锁版本号**，兑换时 `stock = stock - n AND version = version + 1` |
| created_at / updated_at | datetime | NOT NULL | 时间 |

### 17.10 exchange_order（兑换订单）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK | 订单 id |
| order_no | varchar(24) | UNIQUE, NOT NULL | 订单号 |
| user_id | bigint | FK, NOT NULL | 用户 |
| product_id | bigint | FK, NOT NULL | 商品 |
| quantity | int | NOT NULL, CHECK ≥ 1 | 数量 |
| points_cost | int | NOT NULL | 消耗积分 |
| status | varchar(12) | NOT NULL DEFAULT 'PENDING' | `PENDING`/`COMPLETED`/`CANCELLED` |
| redeem_code | char(8) | UNIQUE, NOT NULL | 核销码 |
| redeem_expire_at | datetime | NOT NULL | 核销码过期时间 |
| verified_by | bigint | FK → recycler.id | 核销人 |
| verified_site_id | bigint | FK → site.id | 核销站点 |
| verified_at | datetime | | 核销时间 |
| cancel_reason | varchar(200) | | 取消原因 |
| cancelled_by | bigint | FK → admin_user.id | 取消人 |
| idempotency_key | varchar(64) | UNIQUE(user_id, key) | 幂等键 |
| created_at | datetime | NOT NULL | 下单时间 |

索引：`uk_redeem_code (redeem_code)`、`idx_user_created (user_id, created_at DESC)`、`idx_status (status)`。

### 17.11 announcement（公告）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK | 公告 id |
| title | varchar(50) | NOT NULL | 标题 |
| type | varchar(12) | NOT NULL | `NOTICE`/`KNOWLEDGE` |
| content | text | NOT NULL | 正文（纯文本） |
| cover_url | varchar(255) | | 封面 |
| status | varchar(12) | NOT NULL DEFAULT 'PUBLISHED' | `PUBLISHED`/`OFFLINE` |
| published_at | datetime | | 发布时间 |
| created_at / updated_at | datetime | NOT NULL | 时间 |

### 17.12 appeal（申诉）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK | 申诉 id |
| user_id | bigint | FK, NOT NULL | 申诉人 |
| target_type | varchar(10) | NOT NULL | `DELIVERY`/`EXCHANGE` |
| target_id | bigint | NOT NULL | 关联对象 id |
| description | varchar(500) | NOT NULL | 问题描述 |
| status | varchar(12) | NOT NULL DEFAULT 'PENDING' | `PENDING`/`ACCEPTED`/`REJECTED` |
| conclusion | varchar(500) | | 处理结论 |
| handled_by | bigint | FK → admin_user.id | 处理人 |
| handled_at | datetime | | 处理时间 |
| created_at | datetime | NOT NULL | 提交时间 |

---

## 18. 状态机与关键约束（实现校验清单）

| 对象 | 允许的流转 | 违反时返回 |
| --- | --- | --- |
| delivery_record.status | `NORMAL → REVOKED` | `1006` / `3008` |
| exchange_order.status | `PENDING → COMPLETED`、`PENDING → CANCELLED` | `3012` |
| recycler.status | `PENDING → APPROVED`、`PENDING → REJECTED`、`APPROVED ⇄ DISABLED` | `1006` |
| user.status | `ACTIVE ⇄ DISABLED` | `1006` |
| product.status | `ON ⇄ OFF` | `1006` |

**实现必须满足的 7 条硬约束**

1. 用户端不存在改积分的接口；请求体中的 `points`、`pointsBalance`、`role` 一律忽略（BR-01）。
2. `ADMIN` 调 `POST /api/deliveries`、`POST /api/exchanges/redeem` 直接 403（BR-04）。
3. 回收员查询强制 `recycler_id = 当前登录人`（BR-05）。
4. 兑换的积分扣减与库存扣减必须原子，库存用乐观锁版本号或 Redis 锁（BR-07）。
5. 兑换必须校验 `idempotencyKey`，同 key 返回首笔结果（BR-08）。
6. 核销码一次性，重复核销返回 `3005`（BR-09）。
7. 所有撤销/调整/取消操作必须写操作人、原因、时间（可追溯）。

---

## 19. 阶段③测试要点（供 `tests/postman/` 使用）

| 类型 | 用例 | 期望 |
| --- | --- | --- |
| 越权 | 用户 Token 调 `GET /api/admin/accounts/users` | 403 / `1003` |
| 越权 | 回收员 Token 调 `POST /api/deliveries` 且请求体带他人 `recyclerId` | 服务端忽略该字段，流水归属登录人，不出现他人数据 |
| 越权 | 回收员 A 查回收员 B 的流水 | 列表为空，不返回 B 的数据 |
| 越权 | 管理员调 `POST /api/exchanges/redeem` | 403 / `1003` |
| 边界 | `POST /api/exchanges` 积分恰好等于 `pointsCost` | 成功，余额为 0 |
| 边界 | `POST /api/exchanges` 积分差 1 分 | `3001` |
| 边界 | 库存 1 时 `quantity=2` | `3002` |
| 边界 | 重量 `0.01` / `100` / `100.01` | 前两个成功（0.01 kg 得 1 分），后者 `3010` |
| 边界 | 撤销第 301 秒 | `3007` |
| 并发 | 库存 1，20 并发 `POST /api/exchanges` | 成功 1 笔，其余 `3002`，库存 ≥ 0 |
| 并发 | 余额 100、商品 60 积分，同一用户 10 并发（不同幂等键） | 成功 1 笔，其余 `3001` |
| 幂等 | 同一 `idempotencyKey` 连发 5 次 | 只生成 1 笔订单，返回同一 `orderNo` |

---

## 20. 待确认与风险

| 编号 | 事项 | 当前取值 | 需要确认的人 |
| --- | --- | --- | --- |
| Q-1 | 积分取整方式 | `round(weight × pointsPerKg)`，最低 1 分 | 邓裕达 |
| Q-2 | 核销码有效期 | 30 天 | 杨光磊 |
| Q-3 | `userCode` 是否随注册生成 | 是，6 位数字，全局唯一 | 陈益苇 |
| Q-4 | 上传存储 | 本地磁盘 + `/uploads` 静态目录 | 陈益苇 |
| Q-5 | 令牌黑名单（登出即刻失效） | `v0.1` 不做，前端清令牌 | 邓裕达 |
| Q-6 | 统计看板是否需要实时推送 | 不推送，进页面拉取 | 杨光磊 |
| Q-7 | 公告是否支持富文本 | 纯文本 + 换行，避免 XSS 与额外依赖 | 魏鸿 |
| Q-8 | 是否需要操作审计表 | 建议 P2，先保证流水/明细可追溯 | 全员 |

---

## 21. 附：Mock 数据建议（阶段②）

`mocks/` 下建议提供以下文件，键名与本契约字段一致：

| 文件 | 内容 |
| --- | --- |
| `mocks/categories.json` | 4 个类别：废纸 `PAPER` 10 分/kg、塑料 `PLASTIC` 15 分/kg、金属 `METAL` 20 分/kg、旧衣物 `CLOTH` 8 分/kg |
| `mocks/products.json` | 6 个商品，覆盖「库存充足 / 库存 1 / 库存 0 / 已下架」四种状态 |
| `mocks/points-logs.json` | 覆盖 `INCOME`/`EXPENSE`/`REVOKE`/`ADJUST`/`REFUND` 五种类型 |
| `mocks/deliveries.json` | 覆盖 `NORMAL`/`REVOKED` 两种状态 |
| `mocks/orders.json` | 覆盖 `PENDING`/`COMPLETED`/`CANCELLED` 三种状态 |
| `mocks/announcements.json` | 公告 2 条、科普 2 条 |
| `mocks/stats-overview.json` | 与 `mocks/deliveries.json`、`mocks/orders.json` 的统计口径自洽 |
