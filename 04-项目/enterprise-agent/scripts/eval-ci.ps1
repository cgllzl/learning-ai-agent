# Offline evaluation and governance CI gate (no DeepSeek key required)
# Usage: .\scripts\eval-ci.ps1
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$tests = "AgentEvalCaseCatalogTest,AgentEvaluationServiceTest,CiEvaluationGateTest,TrajectoryEvaluatorTest,AgentComparisonReportTest,TrajectoryRecorderTest,McpCompatibilityTest,McpToolTrustPolicyTest,McpRemoteCredentialTest,SecureMcpToolProviderTest,EnterpriseOrderAfterSalesSkillTest"
Write-Host "Running offline evaluation and governance tests: $tests"
mvn test "-Dtest=$tests" "-Dsurefire.useFile=false"
