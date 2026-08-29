<#
.SYNOPSIS
    Creates a consistent SQLite backup using SQLite's online backup API.

.PARAMETER DatabasePath
    SQLite database to back up. Defaults to GAMING_CAFE_DB_PATH or the
    installed ProgramData database.

.PARAMETER OutDir
    Directory for timestamped .db backups. Defaults to the configured backup
    directory under ProgramData.

.NOTES
    Requires sqlite3.exe on PATH. The installed application uses the Java
    DatabaseBackupService instead, so a client does not need sqlite3.exe.
#>
param(
    [string]$DatabasePath = "",
    [string]$OutDir = ""
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
if ([string]::IsNullOrWhiteSpace($OutDir)) {
    $OutDir = [Environment]::GetEnvironmentVariable("GAMING_CAFE_BACKUP_DIR")
}
if ([string]::IsNullOrWhiteSpace($OutDir)) {
    $OutDir = Join-Path $dataDirectory "backup"
}

if (-not (Test-Path -LiteralPath $DatabasePath -PathType Leaf)) {
    throw "SQLite database was not found: $DatabasePath"
}
$sqlite = Get-Command sqlite3.exe -ErrorAction Stop
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$timestamp = Get-Date -Format "yyyy-MM-dd-HHmmss"
$destination = Join-Path $OutDir "gaming-cafe-$timestamp.db"
$temporary = Join-Path $OutDir ".gaming-cafe-$timestamp.db.tmp"
Remove-Item -LiteralPath $temporary -Force -ErrorAction SilentlyContinue
$escapedTemporary = $temporary.Replace("'", "''")

Write-Host "Creating SQLite backup: $DatabasePath -> $destination"
& $sqlite.Source $DatabasePath ".timeout 10000" ".backup '$escapedTemporary'"
if ($LASTEXITCODE -ne 0) {
    Remove-Item -LiteralPath $temporary -Force -ErrorAction SilentlyContinue
    throw "sqlite3 online backup failed with exit code $LASTEXITCODE"
}
if (-not (Test-Path -LiteralPath $temporary -PathType Leaf) -or (Get-Item -LiteralPath $temporary).Length -eq 0) {
    Remove-Item -LiteralPath $temporary -Force -ErrorAction SilentlyContinue
    throw "sqlite3 online backup produced no usable file"
}
Move-Item -LiteralPath $temporary -Destination $destination
Write-Host "Backup complete: $destination"
