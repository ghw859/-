# 灵枢后端 OpenAPI(Swagger) 文档导出脚本
# 用法：先启动后端（8080），然后执行：
#   powershell -ExecutionPolicy Bypass -File .\export-openapi.ps1
# 可选参数：
#   .\export-openapi.ps1 -BaseUrl http://localhost:8080 -OutFile .\openapi.json
param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$OutFile = "$(Split-Path -Parent $MyInvocation.MyCommand.Path)\openapi.json"
)

$docUrl = "$BaseUrl/v3/api-docs"
Write-Host "拉取 OpenAPI 文档: $docUrl"
try {
    $doc = Invoke-RestMethod -Uri $docUrl -Method Get -TimeoutSec 15
} catch {
    Write-Error "拉取失败：请确认后端已启动（$BaseUrl），错误：$($_.Exception.Message)"
    exit 1
}

$doc | ConvertTo-Json -Depth 100 | Set-Content -Path $OutFile -Encoding utf8
Write-Host "已导出: $OutFile"
Write-Host "导入方式："
Write-Host "  - Postman: Import -> File -> 选择 openapi.json"
Write-Host "  - Swagger Editor: https://editor.swagger.io 粘贴文件内容"
Write-Host "  - 在线文档 UI: $BaseUrl/swagger-ui.html"
