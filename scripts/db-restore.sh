#!/usr/bin/env bash
# Restores a SQLite backup after the application has been stopped.
# Usage: ./scripts/db-restore.sh path/to/gaming-cafe-backup.db
set -euo pipefail

if [[ "${1:-}" == "" ]]; then
  echo "Usage: $0 path/to/gaming-cafe-backup.db" >&2
  exit 1
fi
backup_file="$1"
database_path="${GAMING_CAFE_DB_PATH:-}"
if [[ -z "$database_path" ]]; then
  database_path="${PROGRAMDATA:-${HOME:?HOME is required}}/GamingCafe/data/gaming-cafe.db"
fi

if [[ ! -f "$backup_file" || ! -f "$database_path" ]]; then
  echo "Both the backup and target SQLite database must exist." >&2
  exit 1
fi
command -v sqlite3 >/dev/null 2>&1 || {
  echo "sqlite3 is required on PATH for this maintenance script." >&2
  exit 1
}

if [[ -f "${database_path%/*}/../gaming-cafe.lock" ]]; then
  echo "Stop Gaming Cafe before restoring the database." >&2
  exit 1
fi

integrity="$(sqlite3 "$backup_file" "PRAGMA integrity_check;")"
if [[ "$integrity" != "ok" ]]; then
  echo "The backup failed SQLite integrity validation." >&2
  exit 1
fi

read -r -p "Type RESTORE to replace the target database: " confirmation
if [[ "$confirmation" != "RESTORE" ]]; then
  echo "Restore cancelled." >&2
  exit 1
fi

backup_dir="${GAMING_CAFE_BACKUP_DIR:-$(dirname "$database_path")/../backup}"
mkdir -p "$backup_dir"
umask 077
timestamp="$(date +%Y-%m-%d-%H%M%S)"
pre_restore="$backup_dir/before-restore-$timestamp.db"
temporary="$pre_restore.tmp"
sqlite3 "$database_path" ".timeout 10000" ".backup '$temporary'"
if [[ ! -s "$temporary" ]]; then
  rm -f -- "$temporary"
  echo "Could not create the pre-restore safety backup; nothing was restored." >&2
  exit 1
fi
mv -- "$temporary" "$pre_restore"

sqlite3 "$database_path" ".restore '$backup_file'"
echo "Restore complete. Pre-restore safety backup: $pre_restore"
