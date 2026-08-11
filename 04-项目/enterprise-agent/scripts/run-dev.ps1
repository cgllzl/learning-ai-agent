# 本地启动脚本：读取 .env 并运行 Spring Boot
# 用法：.\scripts\run-dev.ps1
$ErrorActionPreference = "Stop"

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
    Write-Host "已加载 .env"
} else {
    Write-Host "未找到 .env，请先复制 .env.example 并填写 DEEPSEEK_API_KEY" -ForegroundColor Yellow
}

# 2. 确保优先使用 JDK 21
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
}

# 3. 启动
Set-Location $root
mvn spring-boot:run
