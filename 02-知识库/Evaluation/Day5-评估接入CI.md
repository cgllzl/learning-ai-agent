# Day 5：把评估结果接入 CI，回归自动跑

> Week 6 ｜ 归档：`05-记录/归档/2026-09-02-Week6-Day5-评估接入CI.md` ｜ CI：`.github/workflows/agent-eval-ci.yml`

## 一、先讲人话：为什么评估要进 CI

我们前面写的评估用例，如果只是偶尔手动跑一次，很容易“今天过了、下周忘了”。CI（持续集成）的价值是：**每次提交代码，自动把这些评估用例再跑一遍**。谁把 Agent 改坏了，合并之前就能看到红灯。

CI 里通常分成两个阶段：

1. **离线门禁**：不需要 API Key，用固定答案快速验证评估逻辑。
2. **真实联调**：需要 DeepSeek Key，验证真实模型输出；通常定时跑或手动跑，避免每次提交都花钱。

## 二、离线门禁：不花钱也能拦住低级错误

新增 `CiEvaluationGateTest`：

```java
@Test
void offlineCoreCasesAllPass() {
    List<AgentEvalResult> results = List.of(
            evaluator.evaluateCorrectness(ORDER_QUERY, "订单 O1001 金额 399 元"),
            evaluator.evaluateCorrectness(MULTI_AGENT_MERGE, "您好，您的订单 O1001 金额 399 元..."),
            evaluator.evaluateCorrectness(TENANT_ISOLATION, "租户 t1 的订单金额 399；租户 t2 的订单金额 1299。"));

    assertThat(results).allSatisfy(result -> assertThat(result.passed()).isTrue());
}
```

它不调用大模型，直接给固定答案，检查「评估器是否能正确判过」。CI 没有 Key 也能跑。

## 三、本地脚本：一条命令跑离线门禁

新增 `scripts/eval-ci.ps1`：

```powershell
.\scripts\eval-ci.ps1
```

它会执行：

```powershell
mvn test -Dtest=AgentEvalCaseCatalogTest,AgentEvaluationServiceTest,CiEvaluationGateTest
```

## 四、GitHub Actions：每次 push / PR 自动跑

新增 `.github/workflows/agent-eval-ci.yml`：

```yaml
name: Agent Eval CI
on:
  push:
  pull_request:
jobs:
  offline-eval:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
          cache: maven
      - name: Run offline evaluation gate
        run: mvn -B test -Dtest=AgentEvalCaseCatalogTest,AgentEvaluationServiceTest,CiEvaluationGateTest
```

这样，任何一次 push 或 PR，GitHub 都会在 Ubuntu 上自动跑离线评估；失败了就红灯。

## 五、学习例子 + 企业例子

### 学习例子：订单查询评估

`CiEvaluationGateLiveTest` 先跑 `ORDER_QUERY`，真实 DeepSeek 返回 O1001 和 399，评估通过。

### 企业例子：客服回访话术评估

同一个测试再跑 `MULTI_AGENT_MERGE`：企业客服查订单并生成回访话术，评估检查 O1001 和 399 是否还在，评估通过。

真实输出：

```text
[CI 评估-学习例子] AgentEvalResult[caseId=ORDER_QUERY, passed=true, ...]
[CI 评估-企业例子] AgentEvalResult[caseId=MULTI_AGENT_MERGE, passed=true, ...]
```

## 六、如何本地测试

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent

# 1) 离线 CI 门禁（无需 DeepSeek Key）
.\scripts\eval-ci.ps1

# 2) 真实联调门禁（需要 DeepSeek Key）
.\scripts\test-live.ps1 -Test CiEvaluationGateLiveTest
```

## 七、Day 5 完成标准

- [x] 评估结果接入 CI
- [x] 有离线门禁脚本与 GitHub Actions 工作流
- [x] 有学习例子和企业例子，并真实调用大模型
