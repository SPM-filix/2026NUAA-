# backend — 后端服务

## 职责

提供三端共用的 REST API：鉴权、投递登记与积分、积分兑换与库存、商品与商城、账号管理、统计看板、文件上传。

## 负责人

| 范围 | 负责人 |
| --- | --- |
| JWT 认证、RBAC、投递登记、兑换并发控制、回收员数据隔离 | 邓裕达 |
| 基础 CRUD、统计聚合、文件上传、测试与部署 | 陈益苇 |

## 协作约定

- 分支：`feature/backend-<描述>`、`fix/backend-<描述>`
- commit scope：`backend`、`auth`、`delivery`、`exchange`、`mall`、`stats`、`upload`
- 所有接口以 [docs/api.md](../docs/api.md) 为准；接口变更必须同 PR 更新该文档
- 技术栈在阶段①确定后补充到这里
- 不提交 `target/`、构建产物和含密钥的配置文件

## 环境

- 数据库连接全部走环境变量：`DB_HOST=db`（compose 服务名）、`DB_PORT=3306`、`DB_NAME`、`DB_USER`、`DB_PASSWORD`
- **任何配置文件都不要写死 `localhost`**：本地和服务器使用同一套 compose 环境
- 本地启动数据库：仓库根目录 `docker compose up -d`，详见 [deploy/README.md](../deploy/README.md)
- `.env` 不入库，仓库只保留 [.env.example](../.env.example)

## 计划结构

脚手架落地后按职责分目录，参考：

```text
backend/
├── src/main/java/...   # config、auth、controller、service、mapper、entity、common
├── src/main/resources/ # 配置、SQL
└── src/test/           # 单元测试与集成测试
```

具体包名与分层在技术选型后确定，不在阶段①之前预建空目录。
