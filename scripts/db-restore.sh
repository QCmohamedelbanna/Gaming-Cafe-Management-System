#!/usr/bin/env bash
# Restores a MySQL database from a backup produced by db-backup.sh.
#
# Usage: ./scripts/db-restore.sh path/to/backup.sql.gz
#
# WARNING: this overwrites every table currently in the target database.
# Reads the same DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD env vars as
# db-backup.sh (see that script for defaults).
set -euo pipefail

if [ "${1:-}" = "" ]; then
    echo "Usage: $0 path/to/backup.sql.gz" >&2
    exit 1
fi

BACKUP_FILE="$1"
if [ ! -f "$BACKUP_FILE" ]; then
    echo "Backup file not found: $BACKUP_FILE" >&2
    exit 1
fi

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-ps_cafe}"
DB_USER="${DB_USER:-ps_user}"
DB_PASSWORD="${DB_PASSWORD:-ps_password}"

echo "About to overwrite ${DB_NAME}@${DB_HOST}:${DB_PORT} with ${BACKUP_FILE}"
read -r -p "Type the database name (${DB_NAME}) to confirm: " CONFIRM
if [ "$CONFIRM" != "$DB_NAME" ]; then
    echo "Confirmation did not match. Aborting." >&2
    exit 1
fi

gunzip -c "$BACKUP_FILE" | MYSQL_PWD="$DB_PASSWORD" mysql \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --user="$DB_USER" \
    "$DB_NAME"

echo "Restore complete."
