# tests — 测试资产

| 层次 | 位置 | 负责人 | 说明 |
| --- | --- | --- | --- |
| 单元测试 | 后端 `backend/src/test/`（37 个） | 模块负责人 | `mvn test` |
| 冒烟脚本 | [smoke/](smoke/) | 邓裕达 | Node 脚本，覆盖主流程与越权 |
| 高并发脚本 | [load/](load/) | 邓裕达 | 库存抢购、幂等、并发核销、积分扣减 |
| 接口测试 | [postman/](postman/) | 陈益苇 | 全量接口、冒烟、联调 |
| 系统测试用例 | [cases/](cases/) | 陈益苇 | 越权、边界值、并发、主流程 |

## 必测项

- **越权**：普通用户 Token 调用管理员接口；回收员查询他人流水
- **边界值**：0 积分兑换、库存为 1 时并发兑换、重量为 0 或极大值
- **并发**：多人同时兑换同一商品，积分不得为负、库存不得超卖
- **状态**：重复核销同一兑换码、撤销已撤销的流水

## 运行方式

后端单元测试：

```bash
cd backend
JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot" mvn test
```

冒烟与并发（后端已启动、Flyway 已建表，Node 18+）：

```bash
ADMIN_INIT_PASSWORD=<启动后端时设置的管理员初始密码> node tests/smoke/smoke-test.mjs
ADMIN_INIT_PASSWORD=<同上> node tests/smoke/concurrency-test.mjs

# 高并发（会创建 51 个测试用户和 2 个测试商品，结果见 docs/reports/高并发测试报告-阶段③.md）
SMOKE_BASE=http://<服务器IP> ADMIN_INIT_PASSWORD=<同上> \
  node tests/load/high-concurrency-test.mjs
```

脚本默认请求 `http://127.0.0.1:8080`，可用 `SMOKE_BASE` 覆盖。每次运行会注册新的用户与回收员，重复运行不冲突；并发脚本需要一件库存为 1 的在售商品。
