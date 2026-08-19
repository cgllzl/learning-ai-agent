# RAG 真实本地 Embedding 入库测试（首次会下载约 90MB ONNX 模型）
# 用法：.\scripts\test-rag-live.ps1
$ErrorActionPreference = "Stop"

# 强制控制台输出 UTF-8，避免中文乱码（IDEA 终端 / PowerShell 5.1）
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}
try { chcp 65001 | Out-Null } catch {}

# 显式开启本地模型测试
$env:RUN_ONNX_TESTS = "1"

$root = Split-Path -Parent $PSScriptRoot
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
}
Set-Location $root
Write-Host "运行 RAG 真实本地 Embedding 入库测试（首次下载模型约 90MB）" -ForegroundColor Cyan
mvn test "-Dtest=RagIngestionLiveTest" "-Dsurefire.useFile=false"