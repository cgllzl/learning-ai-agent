# Offline evaluation and governance CI gate (no DeepSeek key required)
# Usage: .\scripts\eval-ci.ps1
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$tests = "AgentEvalCaseCatalogTest,AgentEvaluationServiceTest,CiEvaluationGateTest,TrajectoryEvaluatorTest,AgentComparisonReportTest,TrajectoryRecorderTest,McpCompatibilityTest,McpToolTrustPolicyTest,McpRemoteCredentialTest,SecureMcpToolProviderTest,EnterpriseOrderAfterSalesSkillTest,WorkbenchRoutingServiceTest,WorkbenchRouteControllerTest,DemoSessionServiceTest,DemoAuthenticationIntegrationTest"
Write-Host "Running offline evaluation and governance tests: $tests"
mvn test "-Dtest=$tests" "-Dsurefire.useFile=false"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$webRoot = Join-Path (Split-Path $root -Parent) "enterprise-agent-web"
if (-not (Test-Path (Join-Path $webRoot "node_modules"))) {
    throw "前端依赖未安装，请先在 enterprise-agent-web 执行 npm install"
}
Push-Location $webRoot
try {
    npm test
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    npm run build
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}

# Skill Script 是纯 Python 标准库预检器；每次提交也检查它的行为边界。
if (Get-Command py -ErrorAction SilentlyContinue) {
    py -3 -m unittest discover -v -s "..\skills\enterprise-order-after-sales\scripts" -p "test_validate_after_sales_request.py"
} else {
    python -m unittest discover -v -s "..\skills\enterprise-order-after-sales\scripts" -p "test_validate_after_sales_request.py"
}
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
