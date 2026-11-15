# 2026NUAA-
此repo用于2026年NUAA软工课程开发大作业

# 开发流程与阶段划分

项目按四个阶段推进，各阶段主责人见 [小组分工.md](小组分工.md) 的「阶段分工对照」，协作方式见 [协作须知.md](协作须知.md)。

| 阶段 | 主要产出 | 出口标准 |
| --- | --- | --- |
| ① 需求确认与系统设计 | 需求分析报告、用例图、功能结构图、E-R 图、详细设计说明书、接口契约 `docs/api.md` | 需求评审通过，`api.md` 冻结 v0.1 |
| ② 并行开发与 Mock 联调 | `backend/`、`user-web/`、`admin-web/` 源码与 `mocks/` | 三端基于同一份契约完成主流程自测 |
| ③ 前后端联调与系统测试 | Postman 集合、测试用例、系统测试报告 | 越权 / 边界值 / 并发用例通过，主要缺陷清零 |
| ④ 部署上线 | `deploy/` 配置与脚本、线上环境、开发报告与答辩 PPT | 线上完整走通投递 → 积分 → 兑换 → 核销 |

文档按阶段归档，索引见 [docs/README.md](docs/README.md)。

# 环境准备

1. 安装开发工具链：JDK 21、Maven（后端），Node 20+（前端）。
2. 安装 Docker Desktop（本机已安装；Windows 还需**管理员执行 `wsl --install` 并重启**才能启动引擎）。
3. 复制并填写环境变量：`cp .env.example .env`，至少设置 `MYSQL_ROOT_PASSWORD`、`DB_PASSWORD`、`JWT_SECRET`。
4. 启动整套服务：`docker compose up -d --build`（db + backend + web）。
5. 本地不想用容器时，可用本机 MySQL 8.0 启动后端，`DB_HOST=127.0.0.1`。

应用内连接数据库使用 compose 服务名 `db`，不要写 `localhost`；宿主机 GUI 调试才用 `127.0.0.1`。

服务器部署步骤（含 Docker 安装、域名/HTTPS、备份与排障）见 [deploy/README.md](deploy/README.md)。
