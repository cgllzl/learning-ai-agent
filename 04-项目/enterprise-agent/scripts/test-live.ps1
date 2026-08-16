# 真实 DeepSeek 联调测试（自动读取 .env 的 Key）
# 用法：.\scripts\test-live.ps1            # 跑全部联调测试
#       .\scripts\test-live.ps1 -Test OrderAgentLiveTest   # 只跑某一个
param([string]$Test = "")

$ErrorActionPreference = "Stop"
`n# 强制控制台输出为 UTF-8，避免中文乱码（IDEA 终端 / PowerShell 5.1）
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}
try { chcp 65001 | Out-Null } catch {}
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env"

# 1. 读取 .env 到当前进程环境变量
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $kv = $line -split "=", 2
            [Environment]::SetEnvironmentVariable($kv[0].Trim(), $kv[1].Trim(), "Process")
        }
    }
    Write-Host "已加载 .env" -ForegroundColor Green
} else {
    Write-Host "未找到 .env，请先复制 .env.example 并填写 DEEPSEEK_API_KEY" -ForegroundColor Yellow
    exit 1
}

# 2. 确保使用 JDK 21
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
}

# 3. 运行联调测试（打印测试输出，便于看到 Agent 回复）
Set-Location $root
$tests = if ($Test) { $Test } else { "OrderAgentLiveTest,StructuredChatServiceLiveTest,StreamingChatServiceLiveTest" }
Write-Host "运行联调测试：$tests" -ForegroundColor Cyan
mvn test "-Dtest=$tests" "-Dsurefire.useFile=false"