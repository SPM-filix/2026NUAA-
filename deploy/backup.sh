#!/usr/bin/env bash
# 数据库备份：保留 7 天，建议加入 crontab 每日执行
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

set -a
. ./.env
set +a

mkdir -p backups
FILE="backups/recycle-$(date +%Y%m%d-%H%M%S).sql"

docker compose exec -T db mysqldump \
  -uroot -p"${MYSQL_ROOT_PASSWORD}" \
  --single-transaction --routines --triggers \
  --databases "${DB_NAME:-recycle}" > "$FILE"

find backups -name 'recycle-*.sql' -mtime +7 -delete
echo "备份完成：$FILE"
