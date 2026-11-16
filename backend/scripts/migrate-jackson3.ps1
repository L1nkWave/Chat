# Chạy trong thư mục D:\Project\SGU-Chat\backend
# Đổi import Jackson 2 sang Jackson 3 (Spring Boot 4) và bỏ các lệnh findAndRegisterModules()
$ErrorActionPreference = "Stop"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

Get-ChildItem -Path . -Recurse -Filter *.java |
    Where-Object { $_.FullName -notmatch '\\target\\' } |
    ForEach-Object {
        $file = $_.FullName
        $text = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)
        $new = $text
        $new = $new.Replace('import com.fasterxml.jackson.databind.ObjectMapper;', 'import tools.jackson.databind.ObjectMapper;')
        $new = $new.Replace('import com.fasterxml.jackson.core.JsonProcessingException;', 'import tools.jackson.core.JacksonException;')
        $new = $new.Replace('import com.fasterxml.jackson.core.type.TypeReference;', 'import tools.jackson.core.type.TypeReference;')
        $new = $new.Replace('catch (JsonProcessingException e)', 'catch (JacksonException e)')
        $new = $new.Replace('throws UnsupportedEncodingException, JsonProcessingException', 'throws UnsupportedEncodingException, JacksonException')
        $new = [regex]::Replace($new, '\r?\n[ \t]*(objectMapper|mapper)\.findAndRegisterModules\(\);', '')
        if ($new -cne $text) {
            [System.IO.File]::WriteAllText($file, $new, $utf8NoBom)
            Write-Host "Đã sửa: $file"
        }
    }

Write-Host "Xong. Kiểm tra lại: Select-String -Path **\*.java -Pattern 'com.fasterxml.jackson.databind|findAndRegisterModules' -Recurse"