# 后端核心模块交接文档

| 项目 | 内容 |
| --- | --- |
| 模块 | 后端核心 + 陈益苇业务模块集成 |
| 负责人 | 邓裕达（核心）、陈益苇（业务模块） |
| 日期 | 2026-11-15 |
| 依据 | `docs/api.md`、`docs/requirements/需求分析报告-v1.md` |
| 状态 | 编译/单测/打包通过；禁用 Flyway 时 Spring 上下文启动验证通过；MySQL 运行时未验证 |

## 1. 已完成清单

### 1.1 接口

| 接口 | 方法 | 路径 | 角色 | 状态 |
| --- | --- | --- | --- | --- |
| AUTH-01 | POST | `/api/auth/users/register` | 匿名 | 已实现 |
| AUTH-02 | POST | `/api/auth/users/login` | 匿名 | 已实现 |
| AUTH-03 | POST | `/api/auth/recyclers/login` | 匿名 | 已实现 |
| AUTH-04 | POST | `/api/auth/admins/login` | 匿名 | 已实现 |
| AUTH-05 | POST | `/api/auth/logout` | 全部 | 已实现（token_version +1） |
| AUTH-06 | PUT | `/api/auth/password` | 全部 | 已实现（强制改密后可调用） |
| AUTH-07 | POST | `/api/auth/recyclers/register` | 匿名 | 已实现（PENDING） |
| REC-01 | GET | `/api/recyclers/me` | RECYCLER | 已实现 |
| ADM-01 | GET | `/api/admins/me` | ADMIN | 已实现 |
| USER-01 | GET | `/api/users/me` | USER | 已实现 |
| USER-03 | GET | `/api/users/me/qrcode` | USER | 已实现（10 分钟 QR） |
| USER-04 | GET | `/api/users/me/points` | USER | 已实现 |
| USER-05 | GET | `/api/users/me/points/logs` | USER | 已实现 |
| USER-06 | GET | `/api/users/me/deliveries` | USER | 已实现 |
| USER-07 | GET | `/api/users/me/exchanges` | USER | 已实现 |
| USER-08 | GET | `/api/users/me/exchanges/{orderId}` | USER | 已实现 |
| DLV-01 | POST | `/api/deliveries` | RECYCLER | 已实现（事务三写） |
| DLV-02 | GET | `/api/users/lookup` | RECYCLER | 已实现（手机号/编号/二维码） |
| DLV-03 | GET | `/api/recyclers/me/deliveries` | RECYCLER | 已实现（强制本人过滤） |
| DLV-04 | POST | `/api/deliveries/{recordId}/revoke` | RECYCLER | 已实现（5 分钟窗口） |
| DLV-05 | GET | `/api/admin/deliveries` | ADMIN | 已实现 |
| DLV-06 | POST | `/api/admin/deliveries/{recordId}/revoke` | ADMIN | 已实现（含 deficit） |
| DLV-07 | GET | `/api/deliveries/{recordId}` | RECYCLER/ADMIN | 已实现（角色过滤） |
| EXC-01 | POST | `/api/exchanges` | USER | 已实现（幂等 + 乐观锁） |
| EXC-02 | GET | `/api/exchanges/{orderId}` | USER/ADMIN | 已实现 |
| EXC-03 | POST | `/api/exchanges/redeem` | RECYCLER | 已实现（一次性核销） |
| EXC-04 | GET | `/api/recyclers/me/redemptions` | RECYCLER | 已实现 |
| EXC-05 | GET | `/api/admin/exchanges` | ADMIN | 已实现 |
| EXC-06 | POST | `/api/admin/exchanges/{orderId}/cancel` | ADMIN | 已实现（退分+恢复库存） |
| EXC-07 | GET | `/api/exchanges/lookup` | RECYCLER | 已实现（核销前预览） |
| SYS-01 | GET | `/api/health` | 匿名 | 已实现 |

### 1.2 基础设施

- 统一响应 `ApiResponse<T>`、分页 `PageResult<T>`。
- `ErrorCode` 覆盖 api.md 第 2 节；`GlobalExceptionHandler` 统一映射（1002→401、1003→403、其余业务错误 HTTP 200）。
- JWT（jjwt 0.12）载荷 `sub/role/ver/type`；`JwtInterceptor` 校验 `token_version`、`must_change_password`、`@RequireRole`。
- MyBatis-Plus 分页插件、驼峰映射、HikariCP。
- Flyway V1 建表、V2 种子数据。
- `AdminInitializer`：`admin_user` 为空时按 `ADMIN_INIT_PASSWORD` 创建 `admin`。
- 单元测试 5 个类（见第 5 节）。

## 2. 模块层集成情况（陈益苇）

已合并到同一应用（保留其 `controller/service/entity/mapper` 分层）：

| 范围 | 接口 | 状态 |
| --- | --- | --- |
| 商品管理 | PRD-01～07 | 已集成 |
| 类别与积分规则 | CAT-01～07 | 已集成 |
| 站点维护 | SITE-01～04 | 已集成 |
| 公告与科普 | ANN-01～07 | 已集成 |
| 文件上传 | FIL-01 | 已集成（本地磁盘，`/uploads` 静态映射） |
| 统计看板 | STA-01～05 | 接口已集成，聚合逻辑为占位（返回 0/空） |
| 账号管理 | ACC-01～11 | 已集成 |
| 管理员申诉处理 | ACC-09/10 | 已集成 |

仍需补：

- USER-02 用户资料修改
- USER-09/10 用户申诉
- STA 真实聚合 SQL（总量、分类占比、趋势、TOP、工作台）
- 文件上传 OSS
- 部署与 Nginx

集成注意：

- 模块层 Mapper 统一加 `Module` 前缀，避免与核心 Mapper 的 Bean 名冲突。
- 模块层使用 `com.nuaa.recycle.common.R`/`PageResult`，核心使用 `ApiResponse`/`common.api.PageResult`，二者 JSON 结构一致。
- `JwtInterceptor` 同时填充核心 `UserContext` 和模块层 `com.nuaa.recycle.common.UserContext`。
- 陈益苇原有的 `schema.sql`、`AuthController`、`HealthController`、`JwtUtil`、`AuthInterceptor`、`WebConfig`、`MyBatisPlusConfig`、`GlobalExceptionHandler` 已删除或合并，避免重复。

## 3. 代码结构

```text
backend/src/main/java/com/nuaa/recycle/
├── RecycleApplication.java
├── common/
│   ├── api/       ApiResponse、PageResult
│   ├── auth/      Role、RequireRole、UserContext、JwtService、JwtInterceptor
│   ├── config/    MybatisPlusConfig、WebMvcConfig、JacksonConfig、PasswordConfig
│   ├── error/     ErrorCode、BizException、GlobalExceptionHandler
│   └── util/      CodeGenerator、MaskUtils
├── auth/          AuthController、MeController、AuthService、AdminInitializer、实体/Mapper/DTO
├── user/          UserController、UserService、DTO
├── catalog/       Site、RecycleCategory、PointsRule、Product 实体与 Mapper
├── delivery/      DeliveryController、DeliveryService、实体/Mapper/DTO
├── exchange/      ExchangeController、ExchangeService、实体/Mapper/DTO
├── controller/    模块层：账号、商品、类别、站点、公告、上传、统计
├── service/       模块层服务
├── entity/        模块层实体
├── mapper/        模块层 Mapper（统一 Module 前缀）
└── system/        HealthController
```

关键设计：

- 控制器只做参数校验与响应包装；事务在 Service。
- 扣积分/扣库存/状态流转全部使用带条件的 `UPDATE`，以影响行数判断结果。
- 兑换用 `TransactionTemplate`，以便在幂等键唯一冲突导致回滚后，另起查询返回首笔订单。

## 4. 数据库

Flyway 位置：`backend/src/main/resources/db/migration/`

- `V1__schema.sql`：13 张表（user、recycler、admin_user、site、recycle_category、points_rule、delivery_record、points_log、product、exchange_order、announcement、appeal、upload_file），含唯一索引、查询索引、`token_version`、`must_change_password`、`product.version`、`(user_id, idempotency_key)` 唯一约束。
- `V2__seed.sql`：4 个类别及积分规则、1 个站点、6 个商品。
- 初始管理员：设置 `ADMIN_INIT_PASSWORD` 后首次启动自动创建 `admin`，不把密码写进 SQL。
- 陈益苇原有的 `db/schema.sql` 已删除，建表统一由 Flyway 管理。

## 5. 测试现状

已写单元测试（不依赖数据库），2026-11-15 全部通过（22 个）：

- `PointsCalculatorTest`：四舍五入、最低 1 分、边界。
- `RevokeWindowTest`：299/300/301 秒。
- `JwtServiceTest`：签发、解析、QR 类型、非法 token、短密钥。
- `CodeGeneratorTest`：用户编号、核销码、流水号/订单号格式。
- `ExchangeServiceTest`：幂等已有订单、积分不足、库存不足、并发幂等冲突回查。
- `DeliveryServiceTest`：手机号脱敏、非法关键字、用户不存在。

未做/待做：

- 集成测试与真实并发测试（按计划暂缓）。
- MySQL/Flyway 启动验证（Docker 未安装）。
- 阶段③并发用例：库存 1 时 20 并发、余额 100 兑换 60 分商品 10 并发、同一幂等键连发 5 次、并发撤销同一流水。

## 6. 环境与启动

- JDK 21.0.13、Maven 3.9.16 已安装；`JAVA_HOME` / `MAVEN_HOME` 已设置。
- 当前 Claude 会话可能仍继承 Java 8 PATH，执行 Maven 时显式设置 `JAVA_HOME`。
- 环境变量：`DB_HOST`（默认 `db`）、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD`、`JWT_SECRET`（≥32 字节）、`JWT_EXPIRE_MINUTES`、`ADMIN_INIT_PASSWORD`。
- Docker Desktop 未安装，MySQL 未启动；安装后：

```bash
docker compose up -d
cd backend
JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot" mvn spring-boot:run
```

本地 IDE 运行需把 `DB_HOST` 改为 `127.0.0.1`（宿主机端口），容器内仍用 `db`。

## 7. 与契约的差异/说明

1. **幂等键永久唯一**：`uk_user_idem` 不会在 24 小时后释放，比契约的 24 小时更严格；同一 key 永不重复成交。若必须支持 24 小时后复用，需要增加清理任务或分离幂等表。
2. **管理员重置密码已集成**：模块层 `AccountController` 设置 `must_change_password` 并递增 `token_version`，核心拦截器负责强制改密。
3. **文件上传已集成**：模块层使用本地磁盘与 `/uploads` 静态映射；OSS 未实现。
4. **统计接口为占位**：`StatsService` 目前返回 0/空列表，需补真实聚合 SQL。
5. **两套模型并存**：核心使用 `auth/catalog/delivery/exchange` 分包，模块层使用 `controller/service/entity/mapper`；新增功能时应沿用所属层，避免再建同名 Mapper。
6. 未引入 Redis；并发控制依赖 MySQL 条件更新与乐观锁，符合当前规模。

## 8. 已知风险与 TODO

- 已通过编译、单元测试、打包；已用“禁用 Flyway + 不连接数据库”验证 Spring 上下文可启动，未在 MySQL 上验证 Flyway 脚本与 SQL 方言；CHECK 约束需 MySQL 8.0.16+。
- 运行时验证（注册/投递/兑换/核销）需先安装 Docker 并启动 MySQL。
- `StatsService` 真实聚合、用户申诉、用户资料修改、OSS 上传仍待补。新增 Mapper 时注意不要与同名接口冲突（模块层已统一 `Module` 前缀）。
- JWT 拦截器抛出的异常依赖 `@RestControllerAdvice` 处理；若实际运行中未被捕获，需要在 `preHandle` 内直接写响应或增加 `HandlerExceptionResolver`。
- `ADMIN_INIT_PASSWORD` 尚未写入本地 `.env`，启动前需补充。
- 阶段③必须补并发测试，单元测试无法证明 MySQL 行锁行为。
