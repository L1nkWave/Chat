# Chạy trong D:\Project\SGU-Chat\backend
# Thay AntPathRequestMatcher (đã bị xóa ở Spring Security 7) bằng PathPatternRequestMatcher
$ErrorActionPreference = "Stop"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

Get-ChildItem -Path . -Recurse -Filter *.java |
    Where-Object { $_.FullName -notmatch '\\target\\' } |
    ForEach-Object {
        $file = $_.FullName
        $text = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)
        if ($text -notmatch 'AntPathRequestMatcher') { return }
        $new = $text
        $new = $new.Replace('import org.springframework.security.web.util.matcher.AntPathRequestMatcher;', 'import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;')
        $new = [regex]::Replace($new, 'new AntPathRequestMatcher\(("[^"]*"),\s*([\w.]+)\.name\(\)\)', 'PathPatternRequestMatcher.withDefaults().matcher($2, $1)')
        $new = [regex]::Replace($new, 'new AntPathRequestMatcher\(("[^"]*")\)', 'PathPatternRequestMatcher.withDefaults().matcher($1)')
        if ($new -cne $text) {
            [System.IO.File]::WriteAllText($file, $new, $utf8NoBom)
            Write-Host "Đã sửa: $file"
        }
    }
Write-Host "Xong."