# backend — 后端服务

## 职责

提供三端共用的 REST API：鉴权、投递登记与积分、积分兑换与库存、商品与商城、账号管理、统计看板、文件上传。

## 负责人

| 范围 | 负责人 |
| --- | --- |
| JWT 认证、RBAC、投递登记、兑换并发控制、回收员数据隔离 | 邓裕达 |
| 商品/类别/站点/公告/上传/统计/账号管理 | 陈益苇 |

## 技术栈

- Spring Boot 3.3.x + JDK 21 + Maven 3.9+
- MyBatis-Plus 3.5.9（含 jsqlparser、乐观锁插件）
- Flyway（MySQL 8.0）
- JWT（jjwt 0.12）+ BCrypt
- springdoc-openapi（`/swagger-ui.html`）

## 当前状态（2026-11-15）

已实现：

- 核心（`auth/`、`user/`、`catalog/`、`delivery/`、`exchange/`）
  - AUTH-01～07、REC-01、ADM-01、USER-01/03～08
  - DLV-01～07、EXC-01～07、健康检查
  - JWT + `token_version`、强制改密、RBAC、数据隔离
  - Flyway `V1__schema.sql` 建表 + `V2__seed.sql` 种子数据
- 业务模块层（`controller/`、`service/`、`entity/`、`mapper/`）
  - 商品 PRD-01～07、类别与规则 CAT-01～07、站点 SITE-01～04
  - 公告 ANN-01～07、文件上传 FIL-01（本地磁盘）
  - 统计 STA-01～05（接口已接入，聚合逻辑仍为占位）
  - 账号管理 ACC-01～11、管理员申诉处理

未完成/待完善：

- USER-02 用户资料修改、USER-09/10 用户申诉
- 统计接口的真实聚合 SQL
- 文件上传 OSS（当前本地磁盘 + `/uploads` 静态映射）
- Docker/MySQL 运行时验证、Nginx 反代与部署

验证结果：

- `mvn clean compile`、`mvn test`（22 个测试）、`mvn package` 均通过
- 关闭 Flyway、不连接数据库时 Spring 上下文可正常启动（无 Bean/路由冲突）
- MySQL 运行、Flyway 实际建表、接口冒烟仍未验证

详细交接见 [docs/HANDOVER.md](docs/HANDOVER.md)。

## 目录

```text
backend/
├── pom.xml
├── docs/HANDOVER.md
├── src/main/java/com/nuaa/recycle/
│   ├── common/          api、auth、config、error、util
│   ├── auth/            核心：认证、本人资料
│   ├── user/            核心：用户端读取接口、二维码
│   ├── catalog/         核心：类别、规则、站点、商品实体与 Mapper
│   ├── delivery/        核心：投递与撤销
│   ├── exchange/        核心：兑换、核销、取消
│   ├── system/          健康检查
│   ├── controller/      模块层：账号、商品、类别、站点、公告、上传、统计
│   ├── service/         模块层服务
│   ├── entity/          模块层实体
│   └── mapper/          模块层 Mapper（统一 Module 前缀，避免 Bean 名冲突）
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/V1__schema.sql、V2__seed.sql
└── src/test/java/com/nuaa/recycle/   单元测试
```

## 构建与运行

```bash
cd backend
JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot" mvn clean compile
JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot" mvn test
JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot" mvn -DskipTests package
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
| `UPLOAD_PATH` | 上传目录，默认 `./uploads`；容器内建议 `/app/uploads` |

**任何配置都不得写死 `localhost`**；`.env` 不入库，仓库只保留 [.env.example](../.env.example)。

## 接口契约

所有接口以 [docs/api.md](../docs/api.md) 为准；接口变更必须同 PR 更新该文档。
