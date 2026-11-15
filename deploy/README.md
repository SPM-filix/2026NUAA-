# deploy — 服务器部署

目标环境：阿里云 / 腾讯云学生机（Ubuntu 22.04/24.04，2C2G 起），Docker Compose 单机部署。

> 验证状态（2026-11-15）：`docker compose config` 校验通过，两个前端按 `/admin/` base 构建通过；本机 Docker 引擎因 WSL2 未启用无法运行，**镜像构建与容器启动尚未在真实 Docker 中验证**，首次上服务器时请预留排障时间。

## 1. 架构

```text
Internet
   │ 80 / 443
   ▼
[nuaa-recycle-web]  nginx:1.27-alpine          ← 用户端 /、管理端 /admin/
   │  /api/、/uploads/ 反向代理
   ▼
[nuaa-recycle-backend]  Spring Boot 3 / JDK 21
   │  仅 compose 内网
   ▼
[nuaa-recycle-mysql]  MySQL 8.0（命名卷 mysql_data，不映射公网）
```

| 路径 | 说明 |
| --- | --- |
| [../compose.yaml](../compose.yaml) | db + backend + web 三个服务 |
| [web/Dockerfile](web/Dockerfile) | 多阶段构建两个前端并打包进 nginx |
| [nginx.conf](nginx.conf) | SPA 回退、`/api` 与 `/uploads` 反代、静态缓存 |
| [deploy.sh](deploy.sh) | 拉取 → 构建 → 启动 → 健康检查 |
| [backup.sh](backup.sh) | mysqldump 备份，保留 7 天 |
| [../backend/Dockerfile](../backend/Dockerfile) | Maven 构建 + JRE 运行 |
| `mysql/init/` | 仅在数据卷首次创建时执行；建表由 Flyway 负责，目前为空 |

两个前端打包进同一个 nginx：用户端在 `/`，管理端在 `/admin/`（路由 base 已按 `import.meta.env.BASE_URL` 适配）。接口统一走 `/api`，前端不需要改地址。

## 2. 服务器准备

1. 购买服务器，安全组只放行 **22、80、443**；不要放行 3306。
2. 登录服务器安装 Docker（官方脚本，Ubuntu/Debian）：

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"
# 重新登录使 docker 组生效
docker version && docker compose version
```

3. 建议开启 swap（2G 内存机器构建前端时更稳）：

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

## 3. 首次部署

```bash
git clone https://github.com/SPM-filix/2026NUAA-.git
cd 2026NUAA-
cp .env.example .env
```

编辑 `.env`，**服务器上的值必须重新生成**：

```bash
# 生成随机密钥
openssl rand -base64 48   # 填入 JWT_SECRET（≥32 字节）
openssl rand -base64 24   # 填入 DB_PASSWORD / MYSQL_ROOT_PASSWORD
```

```dotenv
DB_HOST=db
DB_PORT=3306
DB_NAME=recycle
DB_USER=recycle
DB_PASSWORD=<强随机密码>
MYSQL_ROOT_PASSWORD=<另一个强随机密码>
JWT_SECRET=<强随机长字符串>
JWT_EXPIRE_MINUTES=120
ADMIN_INIT_PASSWORD=<首次登录用的管理员密码，上线后立即修改>
UPLOAD_PATH=/app/uploads
```

然后发布：

```bash
bash deploy/deploy.sh
```

脚本会构建镜像、启动服务，并轮询 `http://127.0.0.1/api/health`。首次构建需要拉取 Maven/npm 依赖，约 5–15 分钟。

验证：

```bash
curl http://127.0.0.1/api/health
# {"code":0,...,"status":"UP"}

docker compose ps
```

浏览器访问：

- 用户端：`http://<服务器IP>/`
- 管理端：`http://<服务器IP>/admin/`
- 管理员账号 `admin`，密码为 `.env` 中的 `ADMIN_INIT_PASSWORD`（仅在 `admin_user` 为空时创建）

## 4. 日常更新

```bash
cd ~/2026NUAA-
git pull
bash deploy/deploy.sh
```

回滚：

```bash
git log --oneline -5
git checkout <上一个提交>
bash deploy/deploy.sh
```

数据库结构变更由 Flyway 在 backend 启动时自动执行，禁止手工改线上表。

## 5. 绑定域名与 HTTPS

1. 域名添加 A 记录指向服务器公网 IP。
2. 最简方案：域名接入 Cloudflare，开启代理（橙色云朵），SSL/TLS 设为 **Flexible** 或 **Full**。此时源站仍是 HTTP，够课程项目使用。
3. 直接在服务器签证书（不使用 CDN 时）：

```bash
sudo apt-get install -y certbot
# 先停掉 web 容器释放 80 端口
docker compose stop web
sudo certbot certonly --standalone -d your.domain.com
```

然后把证书以只读方式挂进 web 容器，并在 `deploy/nginx.conf` 增加 `listen 443 ssl` 的 server 块，同时在 compose 中映射 `443:443` 与证书目录。涉及改动较多，建议有域名时优先用 Cloudflare 终止 TLS。

## 6. 备份与恢复

手动备份：

```bash
bash deploy/backup.sh
```

定时任务（每天 03:30）：

```bash
crontab -e
30 3 * * * cd /home/<user>/2026NUAA- && bash deploy/backup.sh >> deploy/backup.log 2>&1
```

恢复：

```bash
docker compose exec -T db mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < backups/recycle-YYYYmmdd-HHMMSS.sql
```

上传文件在后端命名卷 `uploads_data` 中，重建容器不丢；迁移服务器时需同时迁移该卷：

```bash
docker run --rm -v nuaa-recycle_uploads_data:/data -v "$PWD":/out alpine \
  tar czf /out/uploads-backup.tar.gz -C /data .
```

## 7. 排障

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f web
docker compose exec db mysql -urecycle -p recycle -e "SHOW TABLES;"
```

| 现象 | 排查 |
| --- | --- |
| web 502 | backend 未起来：看 `docker compose logs backend`；确认 `DB_HOST=db` |
| 管理端刷新 404 | 检查 nginx 的 `/admin/` 回退与构建是否带 `--base=/admin/` |
| 管理员登录失败 | `ADMIN_INIT_PASSWORD` 只在首次启动创建；已有账号时改 `.env` 不生效 |
| Flyway 报错 | 数据库账号权限不足，或历史表与脚本不一致；不要手改 `flyway_schema_history` |
| 上传 413 | 调整 nginx `client_max_body_size` 与后端 multipart 上限 |

## 8. 安全清单

- `.env` 权限 `chmod 600 .env`，绝不提交仓库。
- MySQL 不对公网暴露；`MYSQL_HOST_PORT` 调试映射只在需要时开启。
- 上线后修改管理员初始密码，删除测试账号。
- 定期 `apt upgrade`，仅开放 22/80/443。
- 备份文件放在 `backups/`（已被 `.gitignore` 排除），不要提交。
