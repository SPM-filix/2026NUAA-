# deploy — 环境与部署

本地和服务器共用根目录的 [compose.yaml](../compose.yaml)。目标环境：阿里云 / 腾讯云学生机，Docker + MySQL + Nginx + 后端运行时。

## 目录

| 路径 | 说明 |
| --- | --- |
| `compose.yaml`（仓库根目录） | MySQL 服务；后端、Nginx 在脚手架落地后加入 |
| `mysql/init/` | 首次创建数据卷时执行的建表 / 初始化脚本 |
| `nginx.conf` | 待阶段④添加：静态站点、反向代理 `/api`、上传文件访问 |
| `deploy.sh` | 待阶段④添加：拉取、构建、启动、健康检查 |
| `server.env.example` | 待阶段④添加：服务器端环境变量模板 |

## 首次启动（本地）

前置：安装 Docker Desktop。

```bash
cp .env.example .env
# 编辑 .env，至少设置 MYSQL_ROOT_PASSWORD、DB_PASSWORD、JWT_SECRET
docker compose up -d
docker compose ps
```

验证数据库：

```bash
docker compose exec db mysql -urecycle -p recycle -e "SELECT VERSION();"
```

## 连接约定

| 场景 | 主机 | 说明 |
| --- | --- | --- |
| 容器内的应用 | `db` | compose 服务名，**不要写 localhost** |
| 宿主机 GUI（Navicat / DataGrip） | `127.0.0.1`，端口 `${MYSQL_HOST_PORT}` | 仅回环地址，不对公网暴露 |
| 其他机器 | 不直接连库 | 通过后端接口访问，不开放 3306 |

上线时后端、Nginx 都加入同一个 compose 网络，数据库只在内网可达，3306 不映射到公网。

## 数据持久化

- 数据存放在 Docker 命名卷 `mysql_data`，`docker compose down` 不会删除数据。
- `docker compose down -v` 会删除数据卷，**仅限开发环境**重置时使用。
- 备份：

  ```bash
  docker compose exec -T db mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
    --databases recycle > backup-$(date +%Y%m%d).sql
  ```

  备份文件已被 `.gitignore` 排除，不要提交。

## 初始化脚本

`mysql/init/` 下的 `.sql` / `.sh` 只在**数据卷首次创建**时执行，按文件名排序。修改后需 `docker compose down -v` 重建才会重新执行。

## 服务器部署

1. 服务器安装 Docker 与 Docker Compose 插件。
2. 上传仓库（或 CI 拉取），在根目录创建 `.env`，填写服务器专用密码，`JWT_SECRET` 必须重新生成。
3. `docker compose up -d --build`。
4. 防火墙只开放 80/443（以及 SSH），不开放 3306。
5. 上线前跑通主流程：注册 → 登录 → 回收员录入 → 积分到账 → 兑换 → 核销。
6. 保留上一版本镜像与备份，出问题可回滚。

## 约束

- `.env`、服务器密码、数据库密码、JWT 密钥、OSS 密钥一律不进仓库。
- 部署步骤变更时同步更新本文件，保证换一台机器可复现。
