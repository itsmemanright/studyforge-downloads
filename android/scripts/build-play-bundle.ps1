param([string]$SigningDirectory = (Join-Path $env:USERPROFILE '.studyforge/signing'), [string]$GradleBinary = '')
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/keytool.exe'))) { throw 'Set JAVA_HOME to JDK 17 before building.' }
$keytool = Join-Path $env:JAVA_HOME 'bin/keytool.exe'
$gradleLauncher = if ($GradleBinary) { [IO.Path]::GetFullPath($GradleBinary) } else { Join-Path $projectDirectory 'gradlew.bat' }
if (-not (Test-Path -LiteralPath $gradleLauncher)) { throw 'Gradle launcher was not found.' }
$signingFolder = [IO.Path]::GetFullPath($SigningDirectory)
$keystore = Join-Path $signingFolder 'studyforge-upload.p12'
$protectedPassword = Join-Path $signingFolder 'upload.password.dpapi'
if (-not (Test-Path -LiteralPath $signingFolder)) {
    New-Item -ItemType Directory -Path $signingFolder | Out-Null
    $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $acl = Get-Acl -LiteralPath $signingFolder
    $acl.SetAccessRuleProtection($true,$false)
    $rule = [Security.AccessControl.FileSystemAccessRule]::new($sid,'FullControl','ContainerInherit,ObjectInherit','None','Allow')
    $acl.AddAccessRule($rule)
    Set-Acl -LiteralPath $signingFolder -AclObject $acl
}
if ((Test-Path -LiteralPath $keystore) -ne (Test-Path -LiteralPath $protectedPassword)) { throw 'Upload signing files are incomplete. Restore the existing key and password; do not generate a replacement key for an already registered app.' }
$newKey = -not (Test-Path -LiteralPath $keystore)
if ($newKey) {
    $bytes = [Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
    $secret = ConvertTo-SecureString ([Convert]::ToBase64String($bytes)) -AsPlainText -Force
    ConvertFrom-SecureString $secret | Set-Content -LiteralPath $protectedPassword -NoNewline -Encoding utf8
} else { $secret = Get-Content -LiteralPath $protectedPassword -Raw | ConvertTo-SecureString }
try {
    $env:STUDYFORGE_UPLOAD_STORE_FILE = $keystore
    $env:STUDYFORGE_UPLOAD_STORE_PASSWORD = [Net.NetworkCredential]::new('', $secret).Password
    $env:STUDYFORGE_UPLOAD_KEY_ALIAS = 'studyforge-upload'
    $env:STUDYFORGE_UPLOAD_KEY_PASSWORD = $env:STUDYFORGE_UPLOAD_STORE_PASSWORD
    if ($newKey) {
        & $keytool -genkeypair -keystore $keystore -storetype PKCS12 -alias $env:STUDYFORGE_UPLOAD_KEY_ALIAS -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=StudyForge Upload' -storepass:env STUDYFORGE_UPLOAD_STORE_PASSWORD -keypass:env STUDYFORGE_UPLOAD_KEY_PASSWORD
        if ($LASTEXITCODE -ne 0) { throw 'Upload key generation failed.' }
    }
    & $keytool -exportcert -rfc -keystore $keystore -alias $env:STUDYFORGE_UPLOAD_KEY_ALIAS -storepass:env STUDYFORGE_UPLOAD_STORE_PASSWORD -file (Join-Path $signingFolder 'upload-certificate.pem')
    if ($LASTEXITCODE -ne 0) { throw 'Upload certificate export failed.' }
    Push-Location $projectDirectory
    try {
        & $gradleLauncher :app:testReleaseUnitTest :app:lintRelease :app:bundleRelease :app:assembleRelease --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Signed release bundle build failed.' }
    } finally { Pop-Location }
} finally {
    Remove-Item Env:STUDYFORGE_UPLOAD_STORE_FILE,Env:STUDYFORGE_UPLOAD_STORE_PASSWORD,Env:STUDYFORGE_UPLOAD_KEY_ALIAS,Env:STUDYFORGE_UPLOAD_KEY_PASSWORD -ErrorAction SilentlyContinue
    if ($null -ne $secret) { $secret.Dispose() }
}
Write-Output 'Signed release bundle is ready: app/build/outputs/bundle/release/app-release.aab'
Write-Output 'The private upload key is outside the source project. Its protected password is bound to this Windows account. Keep it for future updates.'
