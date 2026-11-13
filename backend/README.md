# backend — 后端服务

## 职责

提供三端共用的 REST API：鉴权、投递登记与积分、积分兑换与库存、商品与商城、账号管理、统计看板、文件上传。

## 负责人

| 范围 | 负责人 |
| --- | --- |
| JWT 认证、RBAC、投递登记、兑换并发控制、回收员数据隔离 | 邓裕达 |
| 基础 CRUD、统计聚合、文件上传、测试与部署 | 陈益苇 |

## 当前状态（2026-11-14）

- 已实现：AUTH-01～07、REC-01、ADM-01、USER-01/03～08、DLV-01～07、EXC-01～07、健康检查。
- 已建表：Flyway `V1__schema.sql` 的 13 张表 + `V2__seed.sql` 种子数据。
- 未实现：商品/类别/站点/账号/公告/申诉/统计/上传（交接给陈益苇）。
- 已通过 `mvn clean compile`、`mvn test`（22 个测试）、`mvn package`；MySQL/Flyway 运行时尚未验证。
- 详细交接见 [docs/HANDOVER.md](docs/HANDOVER.md)。

## 技术栈

- Spring Boot 3.3.x + JDK 21 + Maven 3.9+
- MyBatis-Plus 3.5.9（含 jsqlparser）
- Flyway（MySQL 8.0）
- JWT（jjwt 0.12）+ BCrypt
- springdoc-openapi（`/swagger-ui.html`）

## 目录

```text
backend/
├── pom.xml
├── docs/HANDOVER.md
├── src/main/java/com/nuaa/recycle/
│   ├── common/      api、auth、config、error、util
│   ├── auth/        认证、本人资料
│   ├── user/        用户端读取接口、二维码
│   ├── catalog/     类别、规则、站点、商品实体与 Mapper
│   ├── delivery/    投递与撤销
│   ├── exchange/    兑换、核销、取消
│   └── system/      健康检查
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/V1__schema.sql、V2__seed.sql
└── src/test/java/com/nuaa/recycle/   单元测试
```

## 构建与运行

```bash
cd backend
# 当前 Claude 会话可能仍指向 Java 8，显式指定 JDK 21
JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot" mvn -DskipTests compile
JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot" mvn test
```

启动（需先安装 Docker 并启动 MySQL）：

```bash
docker compose up -d
JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot" mvn spring-boot:run
```

## 环境变量

| 变量 | 说明 |
| --- | --- |
| `DB_HOST` | 容器内为 `db`；宿主机 IDE 运行时为 `127.0.0.1` |
| `DB_PORT` | 默认 3306 |
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | 数据库名与账号 |
| `JWT_SECRET` | 至少 32 字节 |
| `JWT_EXPIRE_MINUTES` | 默认 120 |
| `ADMIN_INIT_PASSWORD` | `admin_user` 为空时创建 `admin` 的初始密码 |

**任何配置都不得写死 `localhost`**；`.env` 不入库，仓库只保留 [.env.example](../.env.example)。

## 接口契约

所有接口以 [docs/api.md](../docs/api.md) 为准；接口变更必须同 PR 更新该文档。
