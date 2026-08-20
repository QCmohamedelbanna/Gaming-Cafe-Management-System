#!/usr/bin/env bash
# Dumps the MySQL database to a timestamped, gzip-compressed SQL file.
#
# Usage: ./scripts/db-backup.sh [output-directory]
#
# Reads connection details from the environment, defaulting to the values
# provisioned by the root docker-compose.yml so a local `docker compose up
# mysql` needs no extra configuration:
#   DB_HOST      default: 127.0.0.1
#   DB_PORT      default: 3306
#   DB_NAME      default: ps_cafe
#   DB_USER      default: ps_user
#   DB_PASSWORD  default: ps_password
set -euo pipefail

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-ps_cafe}"
DB_USER="${DB_USER:-ps_user}"
DB_PASSWORD="${DB_PASSWORD:-ps_password}"

OUT_DIR="${1:-backups}"
mkdir -p "$OUT_DIR"

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUT_FILE="$OUT_DIR/${DB_NAME}-${TIMESTAMP}.sql.gz"

echo "Backing up ${DB_NAME}@${DB_HOST}:${DB_PORT} -> ${OUT_FILE}"

MYSQL_PWD="$DB_PASSWORD" mysqldump \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --user="$DB_USER" \
    --single-transaction \
    --routines \
    --triggers \
    "$DB_NAME" | gzip > "$OUT_FILE"

echo "Done: $OUT_FILE ($(du -h "$OUT_FILE" | cut -f1))"
