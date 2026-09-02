# Offline evaluation CI gate (no DeepSeek key required)
# Usage: .\scripts\eval-ci.ps1
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$tests = "AgentEvalCaseCatalogTest,AgentEvaluationServiceTest,CiEvaluationGateTest"
Write-Host "Running offline evaluation tests: $tests"
mvn test "-Dtest=$tests" "-Dsurefire.useFile=false"
