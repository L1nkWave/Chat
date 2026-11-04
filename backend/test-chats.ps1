# Chạy trong D:\Project\SGU-Chat
$login = Invoke-RestMethod -Uri http://localhost:8080/api/v1/auth/login -Method POST -ContentType "application/json" -Body '{"username":"@test1","password":"123456"}'
$token = $login.accessToken
try {
    $r = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/chats?limit=40&offset=0" -Headers @{ Authorization = "Bearer $token" } -UseBasicParsing
    "STATUS: " + $r.StatusCode
    $r.Content
} catch {
    "STATUS: " + $_.Exception.Response.StatusCode.value__
    $_.ErrorDetails.Message
}