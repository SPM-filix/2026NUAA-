# admin-web — 管理端（管理员 / 回收员）

## 职责

- **回收员**：扫码或手机号识别用户、录入垃圾类别与重量、发放积分、核销兑换码、查看个人工作台与流水
- **管理员**：垃圾类别与积分规则、回收站点、商品与库存、账号与审核、业务审计、数据统计大屏

## 负责人

杨光磊

## 技术栈

Vue 3 + Vite + Element Plus + Vue Router + Pinia + ECharts（2026-11-12 确定）。
鉴权令牌、接口字段与错误码严格按 [docs/api.md](../docs/api.md) 处理。

## 本地启动

```bash
cd admin-web
npm install
npm run dev        # http://localhost:5174
npm run build      # 产物输出到 dist/，已被 .gitignore 忽略
```

要求 Node 20.19+（本机 Node 24 已验证）。

### 阶段②：默认使用 Mock

不配置任何环境变量时，`VITE_USE_MOCK` 默认为 `true`：

- Mock 服务在 [src/mock/](src/mock/)，按 `docs/api.md` 的路径、字段和错误码返回数据
- 种子数据读取仓库根目录的 [mocks/](../mocks/)，与用户端共用同一份契约数据
- 数据只存在内存中，**刷新页面即恢复初始状态**；新增的流水、核销、上下架等操作在本次会话内有效

### 演示账号

| 角色 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `admin123` | 全量数据与配置权限 |
| 回收员 | `13900001001` | `recycler123` | 已通过审核，可登记与核销 |
| 回收员 | `13900001003` | `recycler123` | 待审核，登录返回 `2004` |
| 回收员 | `13900001004` | `recycler123` | 已驳回，登录返回 `2004` 并提示原因 |

登录页「阶段②演示账号」区域可一键填充。

### 阶段③：切换真实后端

```bash
cd admin-web
cp .env.example .env
# 编辑 .env：
#   VITE_USE_MOCK=false
#   VITE_API_BASE=/api
#   VITE_PROXY_TARGET=http://localhost:8080
npm run dev
```

`vite.config.js` 会把 `/api` 代理到 `VITE_PROXY_TARGET`；生产环境由 Nginx 反代，前端只依赖 `/api` 前缀。

## 已实现页面

| 页面清单编号 | 路由 | 说明 |
| --- | --- | --- |
| P-A-01 | `/login` | 回收员 / 管理员角色切换登录，审核状态错误提示 |
| P-A-02 | `/profile` | 个人资料、改密入口、退出登录 |
| P-A-03 | `/403` | 越权兜底页 |
| P-B-01 | `/workbench` | 回收员工作台：今日重量 / 积分 / 笔数 / 核销数、7 日趋势、站点排名 |
| P-B-02 | `/workbench/identify` | 扫码 / 手机号 / 6 位编号识别用户 |
| P-B-03 | `/workbench/delivery` | 投递登记：类别卡片、重量校验、实时预估积分、成功后继续下一单 |
| P-B-05 | `/workbench/redeem` | 核销码预览 → 二次确认 → 核销成功，附本人核销记录 |
| P-B-06 | `/workbench/records`（含撤销） | 本人流水筛选、5 分钟窗口内自助撤销、撤销原因必填 |
| P-B-07 | `/workbench/rules` | 只读积分规则与计算示例 |
| P-C-01 | `/dashboard` | 5 张总量卡片、分类占比、7/30 日趋势、TOP 排行、待办、可选 30 秒轮询 |
| P-C-02 | `/config/categories` | 类别增改、启停、删除（被引用时由后端拒绝） |
| P-C-03 | `/config/rules` | 当前规则、规则历史、调价（生效时间与「仅影响此后流水」提示） |
| P-C-04 | `/config/sites` | 站点增改与启停 |
| P-C-05 / P-C-06 | `/mall/products` | 商品增改（含图片上传）、上下架、库存调整（SET / INCR + 原因）、删除 |
| P-C-07 | `/accounts/users` | 用户检索、禁用 / 启用、重置为一次性临时密码 |
| P-C-08 | `/accounts/recyclers` | 待审 / 已通过 / 已驳回 / 已禁用分栏，通过时分配站点，驳回填原因 |
| P-C-09 | `/audit/deliveries` | 全量流水、汇总条、异常标记与标红、授权撤销 |
| P-C-10 | `/audit/points` | 远程检索用户、补分 / 扣分、余额预览与二次确认 |
| P-C-11 | `/audit/orders` | 订单筛选、详情、待核销订单取消（退分 + 恢复库存） |
| P-C-12 | `/audit/appeals` | 申诉列表 / 详情、采纳或驳回、跳转积分调整 |
| P-C-13 | `/content/announcements` | 公告增改、预览、发布 / 下线、删除 |

## 目录结构

```text
admin-web/
├── public/covers/        # 商品与公告使用的本地 SVG 封面
├── src/
│   ├── api/              # 按模块封装的接口，统一解包 { code, message, data }
│   ├── components/       # PageHeader / StatusTag / StatCard / BaseChart / UploadImage
│   ├── layouts/          # 侧边栏 + 顶栏布局，按角色渲染菜单
│   ├── mock/             # 阶段② Mock 服务与内存数据仓库
│   ├── router/           # 路由表、角色守卫、菜单配置
│   ├── stores/           # Pinia 鉴权状态（token / role / profile）
│   ├── styles/           # 设计令牌与 Element Plus 主题覆盖
│   ├── utils/            # 字典、格式化、本地令牌存储
│   └── views/
│       ├── recycler/     # 回收员页面
│       └── admin/        # 管理员页面
├── .env.example
├── index.html
└── vite.config.js
```

## 协作约定

- 分支：`feature/admin-web-<描述>`、`fix/admin-web-<描述>`
- commit scope：`admin-web`
- 两种角色共用一套前端，按 RBAC 区分菜单与路由；**前端隐藏入口不构成权限控制，后端必须校验**
- 回收员只能看到本人流水；管理员不参与称重录入与核销（职责分离）
- 接口以 [docs/api.md](../docs/api.md) 为准，开发期用 [mocks/](../mocks/)
- 不提交 `node_modules/`、`dist/`、`.env`

## 实现要点（评审对照）

- 统一响应处理：只按 `code` 分支；`1002` 清空令牌并跳登录，`1003` 展示无权提示
- `mustChangePassword=true` 时路由强制进入改密页，改密成功后清除本地令牌重新登录
- 回收员撤销按钮由服务端返回的 `revocable` 控制，前端不自行推算 5 分钟窗口
- 兑换核销必须先预览再二次确认；上下架、禁用、撤销、积分调整均有二次确认
- 管理员重置密码只展示一次性临时密码，并提供复制按钮；不提供直接设置密码的入口
- 调价弹窗明确提示「仅影响此后新流水」，与 BR-02 / BR-14 一致
