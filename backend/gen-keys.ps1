# Tạo cặp khóa RSA mới cho auth-service và copy khóa public sang các service xác thực JWT.
# Chạy từ thư mục bất kỳ:   .\backend\scripts\gen-keys.ps1
# Yêu cầu: openssl (đã có sẵn trong Git for Windows).

$ErrorActionPreference = "Stop"

if (-not (Get-Command openssl -ErrorAction SilentlyContinue)) {
    throw "Không tìm thấy openssl. Cài Git for Windows (có kèm openssl) rồi mở lại PowerShell."
}

$backend = Split-Path -Parent $PSScriptRoot
$authKeys = Join-Path $backend "auth-service\src\main\resources\keys"
New-Item -ItemType Directory -Force -Path $authKeys | Out-Null

$accessPriv  = Join-Path $authKeys "access_private_key.pem"
$accessPub   = Join-Path $authKeys "access_public_key.pem"
$refreshPriv = Join-Path $authKeys "refresh_private_key.pem"
$refreshPub  = Join-Path $authKeys "refresh_public_key.pem"

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out $accessPriv
openssl rsa -pubout -in $accessPriv -out $accessPub
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out $refreshPriv
openssl rsa -pubout -in $refreshPriv -out $refreshPub

# Khóa public access phải giống nhau ở mọi service kiểm tra JWT
$services = @("user-service", "chat-service", "ws-server")
foreach ($service in $services) {
    $dir = Join-Path $backend "$service\src\main\resources\keys"
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    Copy-Item $accessPub (Join-Path $dir "access_public_key.pem") -Force
}

Write-Host "Đã tạo khóa mới cho auth-service và copy khóa public sang: $($services -join ', ')"
Write-Host "Khởi động lại mọi service để nhận khóa mới. Token cũ sẽ không còn hợp lệ."