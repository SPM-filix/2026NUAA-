#!/usr/bin/env bash
# 服务器一键发布：拉取代码 -> 构建镜像 -> 启动 -> 健康检查
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [ ! -f .env ]; then
  echo "缺少 .env，请先 cp .env.example .env 并填写密钥" >&2
  exit 1
fi

if [ -d .git ]; then
  git pull --ff-only
fi

docker compose build
docker compose up -d --remove-orphans

for _ in $(seq 1 60); do
  if curl -fsS "http://127.0.0.1/api/health" >/dev/null 2>&1; then
    echo "部署完成：http://<服务器IP>/ ，管理端 http://<服务器IP>/admin/"
    exit 0
  fi
  sleep 2
done

echo "健康检查失败，输出最近日志：" >&2
docker compose ps
docker compose logs --tail=100 backend web
exit 1
