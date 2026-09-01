# Day 2：实现自动化评估脚本（正确性 / 引用准确性）

> Week 6 ｜ 归档：`05-记录/归档/2026-09-01-Week6-Day2-自动化评估脚本.md` ｜ 代码：`com.enterprise.agent.evaluation.AgentEvaluationService`

## 一、先讲人话：自动化评估在做什么

Day 1 我们列好了评估用例清单。但「有清单」还不够，还要能**自动判断一条用例过没过**。这就是 Day 2 的自动化评估。

传统程序里，我们写 `assertEquals(expected, actual)`。Agent 的输出是自然语言，不能直接比字符串相等，所以要用更灵活的检查：

- **正确性**：回答里是否包含关键事实。
- **引用准确性**：如果回答带了 `[1]`、`[2]` 这样的引用，它们是否真的指向正确文档。

## 二、正确性：检查关键事实有没有出现

核心逻辑很简单：

```java
public AgentEvalResult evaluateCorrectness(AgentEvalCase evalCase, String answer) {
    List<String> missing = evalCase.expectedChecks().stream()
            .filter(check -> !answer.contains(check))
            .toList();
    boolean passed = missing.isEmpty();
    return new AgentEvalResult(evalCase.id(), passed, passed
            ? "通过：全部预期检查点命中"
            : "未通过：缺少检查点 " + missing);
}
```

解释：

- 用例里的 `expectedChecks` 是「必须出现的关键词」，例如订单查询用 `O1001` 和 `399`。
- 只要回答里两个关键词都在，这条正确性用例就通过。
- 这比「必须逐字相等」更适合大模型，因为模型每次措辞不同，但关键事实不该变。

## 三、引用准确性：带编号的引用必须指对地方

RAG 回答经常长这样：

```text
入职满一年享有 5 天年假[1]。
```

`[1]` 必须指向参考资料列表里的第 1 条。这个判断复用 Week 3 已经写好的 `RagEvaluator.citationCorrect`：

```java
public AgentEvalResult evaluateCitationAccuracy(
        AgentEvalCase evalCase,
        String answer,
        List<RetrievedChunk> sources,
        String expectedDocumentId) {
    boolean passed = RagEvaluator.citationCorrect(answer, sources, expectedDocumentId);
    return new AgentEvalResult(evalCase.id(), passed, passed
            ? "通过：所有引用均指向期望文档"
            : "未通过：引用缺失或指向错误文档");
}
```

这个指标非常企业化：**客服不能编制度**。如果回答里写了 `[1]`，但 `[1]` 根本不是年假制度文档，就必须判失败。

## 四、学习例子 + 企业例子

`AgentEvaluationLiveTest` 用真实 DeepSeek 自动跑了两条用例：

| 用例 | 类型 | 输入 | 检查点 |
| --- | --- | --- | --- |
| ORDER_QUERY | 学习例子 | 查询订单 O1001 | O1001、399 |
| MULTI_AGENT_MERGE | 企业例子 | 查单并生成客服回访话术 | O1001、399 |

真实输出：

```text
[评估-订单查询] AgentEvalResult[caseId=ORDER_QUERY, passed=true, ...]
[评估-客服回访] AgentEvalResult[caseId=MULTI_AGENT_MERGE, passed=true, ...]
```

企业例子落到的场景是：客服既要**查得准**，又要**说得得体**。多 Agent 流水线最后生成的客服话术里，订单号 O1001 和金额 399 都不能丢，丢了就是生产事故。

## 五、如何本地测试

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent

# 1) 不调大模型，验证正确性与引用准确性逻辑
mvn test -Dtest=AgentEvaluationServiceTest

# 2) 真实 DeepSeek 自动评估两条用例
.\scripts\test-live.ps1 -Test AgentEvaluationLiveTest
```

## 六、Day 2 完成标准

- [x] 实现自动化评估脚本：正确性 + 引用准确性
- [x] 有学习例子和企业例子
- [x] 真实调用大模型跑通评估
