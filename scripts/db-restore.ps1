<#
.SYNOPSIS
    Restores a SQLite database from a safe .db backup.

.PARAMETER BackupFile
    SQLite backup file created by db-backup.ps1 or the application API.

.PARAMETER DatabasePath
    Target SQLite database. Defaults to GAMING_CAFE_DB_PATH or the installed
    ProgramData database.

.NOTES
    Stop Gaming Cafe before restoring. The script creates a pre-restore backup
    with SQLite's online backup API and never overwrites it automatically.
    Requires sqlite3.exe on PATH.
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,
    [string]$DatabasePath = ""
)

$ErrorActionPreference = "Stop"

function Get-DefaultDataDirectory {
    $programData = [Environment]::GetEnvironmentVariable("ProgramData")
    if ([string]::IsNullOrWhiteSpace($programData)) {
        $programData = [Environment]::GetEnvironmentVariable("PROGRAMDATA")
    }
    if ([string]::IsNullOrWhiteSpace($programData)) {
        $programData = [Environment]::GetFolderPath([Environment+SpecialFolder]::CommonApplicationData)
    }
    return Join-Path $programData "GamingCafe"
}

$dataDirectory = Get-DefaultDataDirectory
if ([string]::IsNullOrWhiteSpace($DatabasePath)) {
    $DatabasePath = [Environment]::GetEnvironmentVariable("GAMING_CAFE_DB_PATH")
}
if ([string]::IsNullOrWhiteSpace($DatabasePath)) {
    $DatabasePath = Join-Path $dataDirectory "data\gaming-cafe.db"
}

if (-not (Test-Path -LiteralPath $BackupFile -PathType Leaf)) {
    throw "Backup file not found: $BackupFile"
}
if (-not (Test-Path -LiteralPath $DatabasePath -PathType Leaf)) {
    throw "Target SQLite database was not found: $DatabasePath"
}
$sqlite = Get-Command sqlite3.exe -ErrorAction Stop

$lockFile = Join-Path $dataDirectory "gaming-cafe.lock"
if (Test-Path -LiteralPath $lockFile) {
    try {
        $lockStream = [System.IO.File]::Open(
                $lockFile,
                [System.IO.FileMode]::Open,
                [System.IO.FileAccess]::ReadWrite,
                [System.IO.FileShare]::None
        )
        $lockStream.Dispose()
    } catch {
        throw "Gaming Cafe appears to be running. Stop it before restoring the database."
    }
}

$integrity = (& $sqlite.Source $BackupFile "PRAGMA integrity_check;") | Out-String
if ($LASTEXITCODE -ne 0 -or $integrity.Trim() -ne "ok") {
    throw "The backup failed SQLite integrity validation: $BackupFile"
}

$confirmation = Read-Host "Type RESTORE to replace the target database"
if ($confirmation -cne "RESTORE") {
    throw "Restore cancelled."
}

$backupDirectory = Join-Path $dataDirectory "backup"
New-Item -ItemType Directory -Force -Path $backupDirectory | Out-Null
$timestamp = Get-Date -Format "yyyy-MM-dd-HHmmss"
$preRestore = Join-Path $backupDirectory "before-restore-$timestamp.db"
$preRestoreTemp = "$preRestore.tmp"
$escapedPreRestoreTemp = $preRestoreTemp.Replace("'", "''")
$escapedBackup = (Resolve-Path -LiteralPath $BackupFile).Path.Replace("'", "''")

& $sqlite.Source $DatabasePath ".timeout 10000" ".backup '$escapedPreRestoreTemp'"
if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $preRestoreTemp -PathType Leaf)) {
    Remove-Item -LiteralPath $preRestoreTemp -Force -ErrorAction SilentlyContinue
    throw "Could not create the pre-restore safety backup. Nothing was restored."
}
Move-Item -LiteralPath $preRestoreTemp -Destination $preRestore

& $sqlite.Source $DatabasePath ".restore '$escapedBackup'"
if ($LASTEXITCODE -ne 0) {
    throw "SQLite restore failed. The pre-restore backup is available at $preRestore"
}
Write-Host "Restore complete. Pre-restore safety backup: $preRestore"
