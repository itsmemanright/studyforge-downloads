param([Parameter(Mandatory=$true)][string]$Destination)
$ErrorActionPreference = 'Stop'
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to JDK 17 first.' }
$keytool = Join-Path $env:JAVA_HOME 'bin/keytool.exe'
$folder = Join-Path $env:USERPROFILE '.studyforge/signing'
$source = Join-Path $folder 'studyforge-upload.p12'
$passwordFile = Join-Path $folder 'upload.password.dpapi'
$output = [IO.Path]::GetFullPath($Destination)
if (Test-Path -LiteralPath $output) { throw 'Choose a new backup filename; existing files are never overwritten.' }
if (-not (Test-Path -LiteralPath (Split-Path $output -Parent))) { throw 'Create the backup destination folder first.' }
$old = Get-Content -LiteralPath $passwordFile -Raw | ConvertTo-SecureString
$chosen = Read-Host 'Choose a portable backup password (12+ characters); keep it separately' -AsSecureString
if ($chosen.Length -lt 12) { throw 'The backup password must have at least 12 characters.' }
try {
    $env:STUDYFORGE_BACKUP_SOURCE_PASSWORD = [Net.NetworkCredential]::new('',$old).Password
    $env:STUDYFORGE_BACKUP_DEST_PASSWORD = [Net.NetworkCredential]::new('',$chosen).Password
    & $keytool -importkeystore -srckeystore $source -srcstoretype PKCS12 -srcalias studyforge-upload -srcstorepass:env STUDYFORGE_BACKUP_SOURCE_PASSWORD -destkeystore $output -deststoretype PKCS12 -destalias studyforge-upload -deststorepass:env STUDYFORGE_BACKUP_DEST_PASSWORD -destkeypass:env STUDYFORGE_BACKUP_DEST_PASSWORD -noprompt
    if ($LASTEXITCODE -ne 0) { throw 'Signing key backup failed.' }
} finally {
    Remove-Item Env:STUDYFORGE_BACKUP_SOURCE_PASSWORD,Env:STUDYFORGE_BACKUP_DEST_PASSWORD -ErrorAction SilentlyContinue
    $old.Dispose(); $chosen.Dispose()
}
Write-Output 'Portable encrypted upload-key backup created. Do not put it in the source archive or share it with testers.'
