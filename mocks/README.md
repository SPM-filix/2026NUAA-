# mocks — Mock 数据（阶段②）

前端在后端接口未完成时，依据 [docs/api.md](../docs/api.md) 与这里的示例响应并行开发。

## 约定

- 文件路径与接口对应，例如 `POST /api/deliveries` → `mocks/deliveries.json`
- 响应必须是完整的统一响应体，字段、类型、枚举与 `api.md` 一致
- 每个接口至少覆盖：正常响应、业务错误（如积分不足）、空列表
- 接口变更时先改 `api.md`，再同步 Mock，并在群里通知前端
- Mock 工具已确定：管理端使用 [admin-web/src/mock/](../admin-web/src/mock/) 内置适配器，按 `docs/api.md` 路由、分页、校验与计算积分；用户端可复用同一份种子数据

## 阶段②种子数据

以下文件均以「完整统一响应体」保存，管理端 Mock 适配器直接读取；用户端实现 Mock 时也可以复用。

| 文件 | 内容 | 覆盖要点 |
| --- | --- | --- |
| [categories.json](categories.json) | 4 个类别 | 废纸 10 / 塑料 15 / 金属 20 / 旧衣物 8 分·kg⁻¹，含当前规则生效时间 |
| [sites.json](sites.json) | 3 个站点 | 2 个启用、1 个停用，用于审核分配与停用校验 |
| [users.json](users.json) | 6 个用户 | 含禁用用户、零积分用户，余额与订单消耗自洽 |
| [recyclers.json](recyclers.json) | 6 个回收员 | 覆盖 `PENDING` / `APPROVED` / `REJECTED` / `DISABLED` |
| [products.json](products.json) | 6 个商品 | 覆盖库存充足 / 库存 1 / 库存 0 / 已下架 |
| [deliveries.json](deliveries.json) | 16 条流水 | 覆盖 `NORMAL` / `REVOKED`，含单笔 60 kg 异常记录 |
| [orders.json](orders.json) | 8 笔订单 | 覆盖 `PENDING` / `COMPLETED` / `CANCELLED`，含超期未核销订单 |
| [points-logs.json](points-logs.json) | 9 条积分明细 | 覆盖 `INCOME` / `EXPENSE` / `REVOKE` / `ADJUST` / `REFUND` |
| [announcements.json](announcements.json) | 4 条公告 | 2 条系统公告、2 条环保科普，含已下线 |
| [appeals.json](appeals.json) | 3 条申诉 | 覆盖 `PENDING` / `ACCEPTED` / `REJECTED` |
| [rules.json](rules.json) | 7 条规则历史 | 3 个类别各有历史版本，用于验证「调价不重算」 |
| [stats-overview.json](stats-overview.json) | 看板汇总 | 与流水、订单口径一致（正常流水 104.70 kg / 1302 分，已完成订单 1600 分） |

演示时钟固定为 **2026-11-13 14:30（UTC+8）**，种子数据的时间点与「今日」统计自洽。管理端 Mock 不保存密码到 JSON，演示账号与重置逻辑在 `admin-web/src/mock/` 内维护。

## 示例

见 [_example.json](_example.json)。

## 阶段③

联调开始后不再以 Mock 为准，转为真实接口；[tests/postman/](../tests/postman/) 负责验证真实响应与契约一致。管理端把 `VITE_USE_MOCK` 设为 `false` 即可整体关闭 Mock。
