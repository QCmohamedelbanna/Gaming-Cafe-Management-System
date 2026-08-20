<#
.SYNOPSIS
    Restores a MySQL database from a backup produced by db-backup.ps1.

.PARAMETER BackupFile
    Path to a .sql.gz backup file.

.NOTES
    WARNING: this overwrites every table currently in the target database.
    Reads the same DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD environment
    variables as db-backup.ps1 (see that script for defaults). Requires
    mysql (the CLI client) on PATH.
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $BackupFile)) {
    Write-Error "Backup file not found: $BackupFile"
    exit 1
}

$DbHost = if ($env:DB_HOST) { $env:DB_HOST } else { "127.0.0.1" }
$DbPort = if ($env:DB_PORT) { $env:DB_PORT } else { "3306" }
$DbName = if ($env:DB_NAME) { $env:DB_NAME } else { "ps_cafe" }
$DbUser = if ($env:DB_USER) { $env:DB_USER } else { "ps_user" }
$DbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "ps_password" }

Write-Host "About to overwrite $DbName@${DbHost}:${DbPort} with $BackupFile"
$confirm = Read-Host "Type the database name ($DbName) to confirm"
if ($confirm -ne $DbName) {
    Write-Error "Confirmation did not match. Aborting."
    exit 1
}

$SqlFile = [System.IO.Path]::GetTempFileName()
$sourceStream = [System.IO.File]::OpenRead($BackupFile)
$gzipStream = New-Object System.IO.Compression.GZipStream($sourceStream, [System.IO.Compression.CompressionMode]::Decompress)
$targetStream = [System.IO.File]::Create($SqlFile)
try {
    $gzipStream.CopyTo($targetStream)
} finally {
    $targetStream.Dispose()
    $gzipStream.Dispose()
    $sourceStream.Dispose()
}

$env:MYSQL_PWD = $DbPassword
try {
    Get-Content $SqlFile -Raw | & mysql --host=$DbHost --port=$DbPort --user=$DbUser $DbName
} finally {
    Remove-Item Env:\MYSQL_PWD
    Remove-Item $SqlFile -ErrorAction SilentlyContinue
}

Write-Host "Restore complete."
