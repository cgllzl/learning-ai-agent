# Offline evaluation and governance CI gate (no DeepSeek key required)
# Usage: .\scripts\eval-ci.ps1
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$tests = "AgentEvalCaseCatalogTest,AgentEvaluationServiceTest,CiEvaluationGateTest,TrajectoryEvaluatorTest,AgentComparisonReportTest,TrajectoryRecorderTest,McpCompatibilityTest,McpToolTrustPolicyTest,McpRemoteCredentialTest,SecureMcpToolProviderTest,EnterpriseOrderAfterSalesSkillTest,WorkbenchRoutingServiceTest,WorkbenchRouteControllerTest"
Write-Host "Running offline evaluation and governance tests: $tests"
mvn test "-Dtest=$tests" "-Dsurefire.useFile=false"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# Skill Script 是纯 Python 标准库预检器；每次提交也检查它的行为边界。
if (Get-Command py -ErrorAction SilentlyContinue) {
    py -3 -m unittest discover -v -s "..\skills\enterprise-order-after-sales\scripts" -p "test_validate_after_sales_request.py"
} else {
    python -m unittest discover -v -s "..\skills\enterprise-order-after-sales\scripts" -p "test_validate_after_sales_request.py"
}
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
