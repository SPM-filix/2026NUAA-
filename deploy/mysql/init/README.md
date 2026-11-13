# MySQL 初始化脚本

> 表结构现由后端 Flyway 管理（`backend/src/main/resources/db/migration/`），本目录不再放 schema。
> 应用启动时 Flyway 自动执行 V1 建表、V2 种子数据。

容器**首次创建数据卷**时，会按文件名顺序执行本目录下的 `.sql`、`.sh`、`.sql.gz`。

- 如需额外的容器级初始化脚本，可放这里；建表不要放这里
- `backend` 的 `AdminInitializer` 会在 `admin_user` 为空时用 `ADMIN_INIT_PASSWORD` 创建初始管理员

## 注意

- 脚本只在数据卷为空时执行；修改后需要 `docker compose down -v` 重建数据卷（会删除数据，仅限开发环境）。
- 不要在本目录放备份文件或含真实密码的数据。
- 字符集统一 `utf8mb4`，时间统一东八区，与 `compose.yaml` 中的服务配置保持一致。
