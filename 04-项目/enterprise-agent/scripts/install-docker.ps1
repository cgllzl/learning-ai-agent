# 一键安装 Docker Desktop（需要管理员权限）
# 用法：右键「Windows PowerShell (管理员)」，执行：
#   cd <项目目录>
#   .\scripts\install-docker.ps1
# 说明：会启用 WSL2 + 虚拟机平台，下载并静默安装 Docker Desktop，可能需要重启。

$ErrorActionPreference = "Stop"

# --- 0. 管理员检查，非管理员自动提权重启 ---
if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "当前不是管理员，正在以管理员身份重新启动……" -ForegroundColor Yellow
    Start-Process powershell.exe -Verb RunAs -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`""
    exit
}

# --- 1. 启用 WSL 与虚拟机平台功能（Windows 10/11） ---
Write-Host "[1/4] 启用 WSL 与 VirtualMachinePlatform 功能……"
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart
dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart

# --- 2. 安装 WSL2 内核更新包 ---
Write-Host "[2/4] 下载 WSL2 内核更新包……"
$wslUpdate = Join-Path $env:TEMP "wsl_update_x64.msi"
curl.exe -L --connect-timeout 30 -o $wslUpdate "https://wslstorestorage.blob.core.windows.net/wslblob/wsl_update_x64.msi"
Start-Process msiexec.exe -ArgumentList "/i `"$wslUpdate`" /quiet /norestart" -Wait

Write-Host "[2.5/4] 设置 WSL 默认版本为 2……"
wsl.exe --set-default-version 2

# --- 3. 下载并静默安装 Docker Desktop ---
Write-Host "[3/4] 下载 Docker Desktop 安装包（约 500MB）……"
$installer = Join-Path $env:TEMP "DockerDesktopInstaller.exe"
curl.exe -L --connect-timeout 30 -o $installer "https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe"
Write-Host "[4/4] 静默安装 Docker Desktop……"
Start-Process $installer -ArgumentList "install --quiet --accept-license --backend=wsl-2" -Wait

Write-Host ""
Write-Host "安装完成！请重启电脑，然后验证：" -ForegroundColor Green
Write-Host "  docker --version"
Write-Host "  docker compose version"
Write-Host "  docker run hello-world"
