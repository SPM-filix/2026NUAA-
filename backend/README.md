# backend — 后端服务

## 职责

提供三端共用的 REST API：鉴权、投递登记与积分、积分兑换与库存、商品与商城、账号管理、统计看板、文件上传。

## 负责人

| 范围 | 负责人 |
| --- | --- |
| JWT 认证、RBAC、投递登记、兑换并发控制、回收员数据隔离 | 邓裕达 |
| **基础 CRUD、统计聚合、文件上传、测试与部署** | **陈益苇** ✅ |

## 陈益苇完成内容清单

### 1. 商品模块 (Product)
- `GET /api/products` — 商品列表（支持搜索、排序、筛选）
- `GET /api/products/{productId}` — 商品详情
- `POST /api/admin/products` — 新增商品
- `PUT /api/admin/products/{productId}` — 编辑商品
- `PATCH /api/admin/products/{productId}/status` — 上下架
- `PATCH /api/admin/products/{productId}/stock` — 调整库存（SET/INCR 模式）
- `DELETE /api/admin/products/{productId}` — 删除商品（有订单关联时禁止删除）

### 2. 类别与站点模块 (Category / Site)
- `GET /api/categories` — 类别列表（含当前生效积分规则）
- `POST /api/admin/categories` — 新增类别（自动创建初始规则）
- `PUT /api/admin/categories/{categoryId}` — 编辑类别
- `DELETE /api/admin/categories/{categoryId}` — 删除类别（有流水引用时禁止删除）
- `PATCH /api/admin/categories/{categoryId}/status` — 启用/停用类别
- `PUT /api/admin/categories/{categoryId}/rule` — 设置积分规则
- `GET /api/admin/categories/{categoryId}/rules` — 规则历史分页
- `GET /api/sites` — 站点列表
- `POST /api/admin/sites` — 新增站点
- `PUT /api/admin/sites/{siteId}` — 编辑站点
- `PATCH /api/admin/sites/{siteId}/status` — 启用/停用站点

### 3. 公告模块 (Announcement)
- `GET /api/announcements` — 公告列表（用户端只返回 PUBLISHED）
- `GET /api/announcements/{announcementId}` — 公告详情
- `POST /api/admin/announcements` — 新增公告
- `PUT /api/admin/announcements/{announcementId}` — 编辑公告
- `PATCH /api/admin/announcements/{announcementId}/status` — 发布/下线
- `DELETE /api/admin/announcements/{announcementId}` — 删除公告
- `GET /api/admin/announcements` — 公告管理列表（管理员全量查询）

### 4. 文件上传模块 (File)
- `POST /api/files` — 文件上传（支持 jpg/png/webp，≤2MB）
- 文件存储路径随机化，防路径穿越
- 按场景（PRODUCT/AVATAR/ANNOUNCEMENT）记录上传日志

### 5. 统计看板模块 (Stats)
- `GET /api/admin/stats/overview` — 总量看板（用户数、回收员数、待处理数等）
- `GET /api/admin/stats/categories` — 分类占比统计
- `GET /api/admin/stats/trend?days=7` — 趋势统计（7/30天）
- `GET /api/admin/stats/top` — TOP 排行（商品/用户）
- `GET /api/recyclers/me/workbench` — 回收员个人工作台

### 6. 账号管理模块 (Account)
- `GET /api/admin/accounts/users` — 用户列表（支持搜索、状态筛选）
- `PATCH /api/admin/accounts/users/{userId}/status` — 禁用/启用用户
- `POST /api/admin/accounts/users/{userId}/password/reset` — 重置密码（生成10位临时密码，24小时过期）
- `GET /api/admin/accounts/recyclers` — 回收员列表
- `POST /api/admin/accounts/recyclers/{recyclerId}/approve` — 通过审核
- `POST /api/admin/accounts/recyclers/{recyclerId}/reject` — 驳回申请
- `PATCH /api/admin/accounts/recyclers/{recyclerId}/status` — 禁用/启用回收员
- `POST /api/admin/accounts/recyclers/{recyclerId}/password/reset` — 重置回收员密码
- `POST /api/admin/points/adjust` — 人工调整积分（写 ADJUST 明细）
- `GET /api/admin/appeals` — 申诉列表
- `POST /api/admin/appeals/{appealId}/handle` — 处理申诉

### 7. 测试与部署
- **Postman 测试集合**：`tests/postman/陈益苇-接口测试.json`
  - 覆盖全部 42+ 个接口的自动化测试用例
  - 包含成功场景、参数校验、越权测试
  - 使用环境变量管理 token 和动态 ID
- **数据库初始化脚本**：`src/main/resources/db/schema.sql`
  - 13 张表的完整建表语句（含索引、约束、注释）

## 协作约定

- 分支：`feature/backend-<描述>`、`fix/backend-<描述>`
- commit scope：`backend`、`auth`、`delivery`、`exchange`、`mall`、`stats`、`upload`
- 所有接口以 [docs/api.md](../docs/api.md) 为准；接口变更必须同 PR 更新该文档
- 技术栈（2026-11-12 确定）：Spring Boot 3 + JDK 21 + Maven，MySQL 8.0；持久层 MyBatis-Plus 或 Spring Data JPA 由后端负责人确定
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

## 编译运行

```bash
# 1. 确保环境变量
export JAVA_HOME=/path/to/jdk-21
export PATH=$JAVA_HOME/bin:$PATH

# 2. 编译
mvn clean compile

# 3. 打包
mvn clean package -DskipTests

# 4. 运行（需先启动 MySQL）
java -jar target/recycle-backend-1.0.0.jar
```
