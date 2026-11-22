# Generates the RSA key pairs for auth-service and copies the public key to the other services.
# Run from the repository root:  powershell -ExecutionPolicy Bypass -File .\backend\gen-keys.ps1
# Requires openssl (Git for Windows includes it). Running again creates new keys; restart every service afterwards.

$ErrorActionPreference = "Stop"

$openssl = (Get-Command openssl -ErrorAction SilentlyContinue).Source
if (-not $openssl) {
    $candidate = "C:\Program Files\Git\usr\bin\openssl.exe"
    if (Test-Path $candidate) { $openssl = $candidate }
}
if (-not $openssl) {
    Write-Error "openssl not found. Install Git for Windows (it includes openssl) and run this script again."
    exit 1
}

$backend = $PSScriptRoot
$authMain = Join-Path $backend "auth-service\src\main\resources\keys"
$authTest = Join-Path $backend "auth-service\src\test\resources\keys"
New-Item -ItemType Directory -Force -Path $authMain, $authTest | Out-Null

# openssl in ra tiến trình (stderr). Với $ErrorActionPreference = "Stop", PowerShell 5.1 coi đó là lỗi và dừng script,
# nên tạm đổi sang "Continue" và kiểm tra mã thoát ($LASTEXITCODE) thay thế.
function Invoke-OpenSsl([string[]]$arguments) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & $openssl @arguments 2>$null
    } finally {
        $ErrorActionPreference = $previous
    }
    if ($LASTEXITCODE -ne 0) { throw "openssl failed: $($arguments -join ' ')" }
}

function New-KeyPair([string]$privatePath, [string]$publicPath) {
    Invoke-OpenSsl @("genpkey", "-algorithm", "RSA", "-pkeyopt", "rsa_keygen_bits:2048", "-out", $privatePath)
    Invoke-OpenSsl @("rsa", "-pubout", "-in", $privatePath, "-out", $publicPath)
}

# auth-service: access and refresh keys, for both main and test
foreach ($dir in @($authMain, $authTest)) {
    New-KeyPair (Join-Path $dir "access_private_key.pem") (Join-Path $dir "access_public_key.pem")
    New-KeyPair (Join-Path $dir "refresh_private_key.pem") (Join-Path $dir "refresh_public_key.pem")
}

# Services that only verify JWT need the public access key; it must match auth-service
foreach ($svc in @("user-service", "chat-service", "ws-server")) {
    $dir = Join-Path $backend "$svc\src\main\resources\keys"
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    Copy-Item (Join-Path $authMain "access_public_key.pem") (Join-Path $dir "access_public_key.pem") -Force
}

# Tests of user-service and chat-service need the access private key to create fake tokens
foreach ($svc in @("user-service", "chat-service")) {
    $dir = Join-Path $backend "$svc\src\test\resources\keys"
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    Copy-Item (Join-Path $authMain "access_public_key.pem") (Join-Path $dir "access_public_key.pem") -Force
    Copy-Item (Join-Path $authMain "access_private_key.pem") (Join-Path $dir "access_private_key.pem") -Force
}

Write-Output "Keys generated. Restart every service so it picks up the new keys."