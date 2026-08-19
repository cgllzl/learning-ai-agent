# RAG 问答端到端联调：真实本地 Embedding 入库 + 真实 DeepSeek 生成
# 用法：.\scripts\test-rag-qa-live.ps1
$ErrorActionPreference = "Stop"

# 强制控制台输出 UTF-8，避免中文乱码
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}
try { chcp 65001 | Out-Null } catch {}

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $kv = $line -split "=", 2
            [Environment]::SetEnvironmentVariable($kv[0].Trim(), $kv[1].Trim(), "Process")
        }
    }
}
$env:RUN_ONNX_TESTS = "1"

if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
}
Set-Location $root
Write-Host "运行 RAG 问答端到端联调（Embedding 入库 + DeepSeek 生成）" -ForegroundColor Cyan
mvn test "-Dtest=RagQaLiveTest" "-Dsurefire.useFile=false"