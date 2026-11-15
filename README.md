# 校园垃圾回收积分系统

2026 年 NUAA 软件工程课程开发大作业。

居民/学生把可回收物送到校园回收站，回收员称重登记后系统按类别系数发放积分，积分可在商城兑换实物礼品。系统覆盖「投递 → 积分 → 兑换 → 核销」完整闭环。

## 在线演示

| 入口 | 地址 |
| --- | --- |
| 用户端 | <http://8.136.245.40/> |
| 管理端（管理员 / 回收员） | <http://8.136.245.40/admin/> |
| 健康检查 | <http://8.136.245.40/api/health> |

- 管理员账号 `admin`，密码由服务器 `.env` 的 `ADMIN_INIT_PASSWORD` 设置，仓库中不保存任何明文密码。
- 当前为单实例 HTTP 部署（阿里云 ECS 2C4G + Docker Compose），适合课程演示；域名与 HTTPS 方案见 [deploy/README.md](deploy/README.md)。

## 三类角色

| 角色 | 入口 | 能做什么 |
| --- | --- | --- |
| 用户（居民/学生） | 用户端 | 注册登录、出示身份二维码/6 位编号、查看投递记录与积分明细、商城兑换、查看公告、提交申诉 |
| 回收员 | 管理端 | 扫码或手机号识别用户、录入类别与重量发放积分、核销兑换码、查看个人工作台与本人流水、5 分钟内撤销 |
| 系统管理员 | 管理端 | 类别与积分规则、商品/库存/上下架、站点、账号与回收员审核、流水与订单审计、申诉处理、数据看板 |

核心约束：积分只能由回收员录入产生；管理员不直接称重和核销；回收员只能看到本人经手数据；兑换的积分扣减与库存扣减必须原子化。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.3.5、JDK 21、Maven、MyBatis-Plus 3.5.9、Flyway、JWT（jjwt）、BCrypt、springdoc-openapi |
| 数据库 | MySQL 8.0（Flyway 管理表结构与种子数据） |
| 用户端 | Vue 3、Vite 6、Element Plus、Pinia、qrcode |
| 管理端 | Vue 3、Vite 7、Element Plus、Pinia、ECharts |
| 部署 | Docker Compose（MySQL + Spring Boot + Nginx）、Nginx 反代与静态站点 |

## 项目结构

```text
2026NUAA-/
├── backend/                 Spring Boot 后端
│   ├── src/main/java/com/nuaa/recycle/
│   │   ├── auth/ user/ catalog/ delivery/ exchange/   # 核心：认证、投递、兑换、并发控制
│   │   ├── controller/ service/ entity/ mapper/       # 模块层：商品、类别、站点、公告、上传、统计、账号
│   │   ├── common/                                    # 统一响应、错误码、JWT、拦截器、配置
│   │   └── dto/                                       # 契约 DTO（如 CategoryResponse）
│   ├── src/main/resources/db/migration/                # Flyway V1 建表、V2 种子数据
│   ├── src/test/                                       # 39 个单元测试
│   ├── docs/HANDOVER.md                                # 核心模块交接文档
│   └── Dockerfile
├── user-web/                用户端（18 个页面，src/mock 可离线 Mock）
├── admin-web/               管理端（管理员 + 回收员，22 个页面）
├── docs/                    需求、接口契约、测试报告（见 docs/README.md）
├── tests/                   单元/冒烟/并发测试资产
│   ├── smoke/               主流程冒烟脚本
│   ├── load/                高并发测试脚本
│   ├── postman/             接口集合
│   └── cases/               测试用例
├── mocks/                   阶段②共享种子数据
├── deploy/                  Docker 部署、Nginx、发布与备份脚本
├── compose.yaml             db + backend + web
├── .env.example             环境变量模板
├── 小组分工.md               成员职责（分工唯一来源）
└── 协作须知.md               分支、commit、PR 约定
```

## 快速开始

### 方式一：Docker Compose（推荐）

前置：Docker 与 Compose 插件。

```bash
cp .env.example .env
# 填写 MYSQL_ROOT_PASSWORD、DB_PASSWORD、JWT_SECRET、ADMIN_INIT_PASSWORD
docker compose up -d --build
```

启动后：

- 用户端 <http://localhost/>
- 管理端 <http://localhost/admin/>
- 管理员 `admin` / `.env` 中的 `ADMIN_INIT_PASSWORD`

数据库表由 Flyway 在后端启动时自动创建，种子数据包含 4 个类别、1 个站点、6 个商品。

### 方式二：本地开发

后端（需要 JDK 21、Maven、MySQL 8.0）：

```bash
cd backend
DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=recycle DB_USER=recycle DB_PASSWORD=*** \
JWT_SECRET=*** ADMIN_INIT_PASSWORD=*** \
JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot" \
  mvn -DskipTests package
JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot" \
  java -jar target/recycle-backend-0.1.0-SNAPSHOT.jar
```

前端：

```bash
cd user-web && npm install && npm run dev    # http://localhost:5173
cd admin-web && npm install && npm run dev   # http://localhost:5174
```

两个前端默认开启 Mock，可脱离后端独立演示。联调时在各自目录创建 `.env.local`：

```dotenv
# user-web/.env.local
VITE_USE_MOCK=false
VITE_API_BASE_URL=/api

# admin-web/.env.local
VITE_USE_MOCK=false
VITE_API_BASE=/api
VITE_PROXY_TARGET=http://127.0.0.1:8080
```

注意：应用连接数据库统一使用 compose 服务名 `db`，**不要在 `application.yml` 或 `.env` 里写 `localhost`**；宿主机本地调试才用 `127.0.0.1`。

## 使用流程

1. **用户**：用户端注册 → 首页出示身份二维码或报 6 位编号。
2. **回收员**：管理端登录 → 扫码/手机号/编号识别用户 → 选择类别、输入重量 → 确认登记，积分自动到账。
3. **用户**：商城选择商品 → 兑换 → 获得 8 位核销码。
4. **回收员**：兑换核销页输入核销码 → 核对订单 → 确认核销。
5. **管理员**：数据看板查看回收量、积分、分类占比、趋势与排行；在流水/订单/账号页做审计与处理。

## 测试

```bash
cd backend && JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot" mvn test
```

- 单元测试：39 个，覆盖积分计算、撤销窗口、JWT、单号生成、兑换幂等/库存、申诉、统计聚合、契约字段。
- 冒烟脚本：`tests/smoke/smoke-test.mjs`，35 项断言。
- 高并发脚本：`tests/load/high-concurrency-test.mjs`，覆盖 50 并发抢库存、幂等键、并发核销、积分扣减。
- 报告：[系统测试报告-阶段③.md](docs/reports/系统测试报告-阶段③.md)、[高并发测试报告-阶段③.md](docs/reports/高并发测试报告-阶段③.md)。

## 部署

服务器部署（Docker 安装、`.env` 密钥生成、`deploy.sh` 一键发布、HTTPS、备份与排障）见 [deploy/README.md](deploy/README.md)。

```bash
git pull
bash deploy/deploy.sh       # 构建 + 启动 + 健康检查
bash deploy/backup.sh       # 数据库备份，保留 7 天
```

## 开发流程

项目按四阶段推进，各阶段主责人见 [小组分工.md](小组分工.md)，协作方式（分支、commit、PR）见 [协作须知.md](协作须知.md)。

| 阶段 | 主要产出 | 出口标准 | 状态 |
| --- | --- | --- | --- |
| ① 需求确认与系统设计 | 需求报告、用例图、功能结构图、E-R 图、接口契约 | `docs/api.md` 冻结 v0.1 | 已完成 |
| ② 并行开发与 Mock 联调 | 三端源码与 `mocks/` | 各端按契约完成主流程自测 | 已完成 |
| ③ 前后端联调与系统测试 | 冒烟、并发、测试报告 | 主要缺陷清零 | 已完成 |
| ④ 部署上线 | `deploy/` 配置与线上环境 | 线上走通主流程 | 已完成（HTTP 单实例） |

## 文档索引

- [docs/README.md](docs/README.md) — 需求、设计、报告索引
- [docs/交接文档-魏鸿.md](docs/交接文档-魏鸿.md) — 阶段④交接（运维、报告、PPT）
- [docs/api.md](docs/api.md) — 接口契约 v0.1
- [backend/docs/HANDOVER.md](backend/docs/HANDOVER.md) — 后端核心交接
- [deploy/README.md](deploy/README.md) — 部署手册
- [tests/README.md](tests/README.md) — 测试资产与运行方式

## 已知限制

- 线上为 HTTP 单实例，未配置域名与 HTTPS；学生机免费试用到期前需关注额度。
- 文件上传为本地磁盘，OSS 未接入。
- 统计为实时聚合 SQL，数据量增大后可考虑缓存或预聚合。
- 生产库中留有阶段③联调产生的测试账号、测试商品与订单，答辩前可按需重置数据卷。
