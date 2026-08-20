<#
.SYNOPSIS
    Dumps the MySQL database to a timestamped, gzip-compressed SQL file.

.PARAMETER OutDir
    Directory to write the backup into. Defaults to .\backups.

.NOTES
    Reads connection details from environment variables, defaulting to the
    values provisioned by the root docker-compose.yml:
      DB_HOST      default: 127.0.0.1
      DB_PORT      default: 3306
      DB_NAME      default: ps_cafe
      DB_USER      default: ps_user
      DB_PASSWORD  default: ps_password

    Requires mysqldump on PATH (ships with MySQL Server / MySQL Workbench)
    and 7-Zip's `7z` or PowerShell's own gzip via .NET for compression;
    this script uses .NET's GZipStream so no extra tools are required.
#>
param(
    [string]$OutDir = "backups"
)

$ErrorActionPreference = "Stop"

$DbHost = if ($env:DB_HOST) { $env:DB_HOST } else { "127.0.0.1" }
$DbPort = if ($env:DB_PORT) { $env:DB_PORT } else { "3306" }
$DbName = if ($env:DB_NAME) { $env:DB_NAME } else { "ps_cafe" }
$DbUser = if ($env:DB_USER) { $env:DB_USER } else { "ps_user" }
$DbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "ps_password" }

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$SqlFile = Join-Path $OutDir "$DbName-$Timestamp.sql"
$GzFile = "$SqlFile.gz"

Write-Host "Backing up $DbName@${DbHost}:${DbPort} -> $GzFile"

$env:MYSQL_PWD = $DbPassword
try {
    & mysqldump --host=$DbHost --port=$DbPort --user=$DbUser `
        --single-transaction --routines --triggers $DbName `
        | Out-File -FilePath $SqlFile -Encoding utf8
} finally {
    Remove-Item Env:\MYSQL_PWD
}

$sourceStream = [System.IO.File]::OpenRead($SqlFile)
$targetStream = [System.IO.File]::Create($GzFile)
$gzipStream = New-Object System.IO.Compression.GZipStream($targetStream, [System.IO.Compression.CompressionMode]::Compress)
try {
    $sourceStream.CopyTo($gzipStream)
} finally {
    $gzipStream.Dispose()
    $targetStream.Dispose()
    $sourceStream.Dispose()
}
Remove-Item $SqlFile

$sizeKb = [math]::Round((Get-Item $GzFile).Length / 1KB, 1)
Write-Host "Done: $GzFile (${sizeKb} KB)"
