# user-web — 用户端（居民 / 学生）

## 职责

注册登录、个人资料与密码、身份二维码、投递记录、积分余额与明细、积分商城与兑换核销码、公告与环保科普。

## 负责人

赵哲禹

## 技术栈

Vue 3 + Vite + Element Plus + Vue Router + Pinia（2026-11-12 确定）。

## 本地运行

```bash
cd user-web
npm install
npm run dev      # http://localhost:5173
npm run build    # 产物在 dist/
```

Node 建议 20+（本机验证使用 Node 22 / npm 10）。

## Mock 模式（阶段②）

默认开启，不依赖后端，直接按 [docs/api.md](../docs/api.md) 契约返回数据：

- 演示账号：`13800001234` / `abc12345`（积分 1581，含投递、兑换、公告、申诉等示例数据）
- Mock 实现：`src/mock/index.js`（接口路由与内存数据库）、`src/mock/data.js`（种子数据）
- 数据持久化在 `localStorage` 的 `nuaa.user.mock.v2`；清空该项即可恢复种子数据
- 关闭 Mock、改连真实后端：复制 `.env.example` 为 `.env.local`，设置 `VITE_USE_MOCK=false`（`/api` 已在 `vite.config.js` 代理到 `http://127.0.0.1:8080`）

Mock 已覆盖用户端全部接口：注册/登录/登出/改密、个人资料、二维码、积分汇总与明细、投递列表、商城列表与详情、创建兑换（含幂等键、库存、积分校验）、兑换列表与详情、创建/查询申诉、公告列表与详情、分类列表、文件上传。

## 页面与路由

| 编号 | 页面 | 路由 | 说明 |
| --- | --- | --- | --- |
| P-U-01 | 登录 | `/login` | 手机号 + 密码，失败展示错误码文案 |
| P-U-02 | 注册 | `/register` | 密码策略、确认密码、协议勾选、手机号查重 |
| P-U-03 | 首页 | `/` | 积分卡片、身份码入口、最近 3 条投递、快捷入口 |
| P-U-04 | 身份二维码 | `/me/qrcode` | 600 秒倒计时自动刷新、编号复制、亮度提示 |
| P-U-05 | 投递记录 | `/me/deliveries` | 类别/日期筛选、加载更多、撤销置灰与原因 |
| P-U-06 | 投递详情 | `/me/deliveries/:id` | 流水与撤销信息、申诉入口预填 |
| P-U-07 | 积分明细 | `/me/points` | 汇总 + 类型筛选，正绿负红，显示变动后余额 |
| P-U-08 | 商城列表 | `/mall` | 搜索、排序、库存 0 置「已兑完」、加载更多 |
| P-U-09 | 商品详情 | `/mall/:id` | 数量上限 `min(库存,5)`、积分不足提示「还差 xx 积分」、幂等键 |
| P-U-10 | 兑换结果 | `/mall/result/:orderId` | 大号核销码、剩余积分、查看记录 |
| P-U-11 | 兑换记录 | `/me/exchanges` | 状态筛选、待核销码、详情弹层（核销人/站点/取消原因） |
| P-U-12 | 公告 / 科普 | `/announcements`、`/announcements/:id` | 分类 tab、摘要、详情图片 |
| P-U-13 | 提交申诉 | `/me/appeals/new` | 关联对象只读、10–500 字计数 |
| P-U-14 | 我的申诉 | `/me/appeals` | 状态标签、点击展开处理结论 |
| P-U-15 | 个人资料 | `/me/profile` | 头像上传（≤2MB）、昵称、地址、手机号只读 |
| P-U-16 | 修改密码 | `/me/password` | 强制改密拦截、成功后清空令牌回登录 |
| - | 我的（底部 Tab） | `/me` | 用户卡片、各记录入口、退出登录 |

底部 Tab 固定三项：首页 `/`、商城 `/mall`、我的 `/me`；其余页面以顶部返回栏进入。

## 目录结构

```text
user-web/
├── src/
│   ├── api/          # request.js（统一响应体/401 处理）+ index.js（按模块封装）
│   ├── components/   # BottomTab / PageHeader / StatusTag / EmptyState
│   ├── composables/  # usePagedList（统一「加载更多」分页）
│   ├── mock/         # Mock 适配器与种子数据
│   ├── router/       # 路由与登录/强制改密守卫
│   ├── stores/       # Pinia 鉴权与用户信息
│   ├── utils/        # 时间格式、枚举文案、会话存储
│   ├── views/        # 16 个页面 + 我的 Tab
│   ├── App.vue
│   ├── main.js
│   └── styles.css
├── .env.example
├── index.html
├── package.json
└── vite.config.js
```

## 接口约定与已知缺口

- 统一响应体 `{code, message, data}`，`code !== 0` 由拦截器统一 toast；HTTP 401 清空会话并回登录页。
- 分页统一 `page` / `size`，列表返回 `{total, list}`；用户端全部使用「加载更多」。
- **契约缺口**：`docs/api.md` 没有用户端投递详情接口（`API-DLV-07` 仅回收员/管理员可用）。当前 `userApi.getDeliveryById` 通过本人投递列表（`size=50`）逐页查找该记录，刷新详情页也可用。若后续后端补充 `GET /api/users/me/deliveries/{recordId}`，只需替换该函数。

## 自测记录

- `npm run build` 通过。
- 浏览器（Mock 模式）实测通过：登录 → 首页积分 1581 → 商城 7 件商品 → 兑换 300 积分（核销码 8 位、剩余 1281）→ 身份二维码（真实二维码 + 102483 编号）→ 投递记录 9 条及详情 → 积分明细 15 条 → 兑换记录 3 笔 → 公告列表 → 个人资料表单。

## 协作约定

- 分支：`feature/user-web-<描述>`、`fix/user-web-<描述>`
- commit scope：`user-web`
- 阶段②只依赖 [docs/api.md](../docs/api.md) 与 [mocks/](../mocks/)，接口未就绪时不要等待后端
- 权限制约：用户不能修改自己的积分余额，不能查看他人数据；前端隐藏入口不等于权限控制，后端必须校验
- 不提交 `node_modules/`、`dist/`、`.env*`
