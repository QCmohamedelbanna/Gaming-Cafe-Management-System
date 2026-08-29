#!/usr/bin/env bash
# Creates a consistent SQLite backup through sqlite3's online backup API.
# The installed Windows client uses the Java backup service and does not need
# this script or sqlite3 on the client machine.
set -euo pipefail

database_path="${GAMING_CAFE_DB_PATH:-}"
if [[ -z "$database_path" ]]; then
  database_path="${PROGRAMDATA:-${HOME:?HOME is required}}/GamingCafe/data/gaming-cafe.db"
fi

backup_dir="${GAMING_CAFE_BACKUP_DIR:-}"
if [[ -z "$backup_dir" ]]; then
  backup_dir="$(dirname "$database_path")/../backup"
fi

if [[ ! -f "$database_path" ]]; then
  echo "SQLite database not found: $database_path" >&2
  exit 1
fi
command -v sqlite3 >/dev/null 2>&1 || {
  echo "sqlite3 is required on PATH for this maintenance script." >&2
  exit 1
}

mkdir -p "$backup_dir"
umask 077
timestamp="$(date +%Y-%m-%d-%H%M%S)"
destination="$backup_dir/gaming-cafe-$timestamp.db"
temporary="$backup_dir/.gaming-cafe-$timestamp.db.tmp"

echo "Creating SQLite backup: $database_path -> $destination"
sqlite3 "$database_path" ".timeout 10000" ".backup '$temporary'"
if [[ ! -s "$temporary" ]]; then
  rm -f -- "$temporary"
  echo "sqlite3 online backup produced no usable file" >&2
  exit 1
fi
mv -- "$temporary" "$destination"
echo "Backup complete: $destination"
