# Day 6：针对失败用例复盘改进

> Week 6 ｜ 归档：`05-记录/归档/2026-09-03-Week6-Day6-失败用例复盘.md` ｜ 代码：`com.enterprise.agent.evaluation.EvalRegressionRunner`

## 一、先讲人话：评估失败之后要做什么

评估不是「跑一遍，红了就完事」。更重要的下一步是：**看懂为什么红，然后改进，再回归验证**。

这个过程可以拆成四步：

1. 找到失败用例；
2. 看失败原因；
3. 做针对性改进；
4. 重新跑评估确认通过。

Day 6 就把这条「失败 → 复盘 → 修复 → 回归」的链路做成代码。

## 二、失败信息怎么表示

新增两个数据结构：

```java
public record EvalFailure(String caseId, String actualAnswer, String reason) {
}
```

```java
public record EvalRunReport(
        int total,
        int passed,
        int failed,
        List<EvalFailure> failures) {

    public boolean success() {
        return failed == 0;
    }
}
```

解释：

- `EvalFailure`：一条失败记录，包含哪个用例失败、实际回答是什么、失败原因是什么。
- `EvalRunReport`：整轮评估的汇总，`success()` 方便判断这轮是否全过。

## 三、回归执行器：一次跑完，自动收集失败

```java
public EvalRunReport run(List<AgentEvalCase> cases,
                         Function<AgentEvalCase, String> answerProvider) {
    int passed = 0;
    List<EvalFailure> failures = new ArrayList<>();

    for (AgentEvalCase evalCase : cases) {
        String answer = answerProvider.apply(evalCase);
        AgentEvalResult result = evaluator.evaluateCorrectness(evalCase, answer);
        if (result.passed()) {
            passed++;
        } else {
            failures.add(new EvalFailure(evalCase.id(), answer, result.detail()));
        }
    }
    return new EvalRunReport(cases.size(), passed, failures.size(), List.copyOf(failures));
}
```

解释：`answerProvider` 是一个函数，表示「某个用例的答案从哪里来」。这样既可以先用固定答案模拟失败，也可以换成真实 Agent 再跑一次。

### 为什么要把“答案来源”当成参数传进去

`Function<AgentEvalCase, String>` 是 Java 的函数式接口：

- 输入类型：`AgentEvalCase`（一条评估用例）
- 输出类型：`String`（这条用例的回答）

`run()` 拿到 `answerProvider` 后，不关心它是固定答案还是真实模型，只调用 `answerProvider.apply(evalCase)` 得到回答。这样以后接别的系统时，`run()` 代码不用改。

### 两种调用写法

1. 固定答案，模拟失败：

```java
runner.run(
    List.of(orderCase),
    ignored -> "订单 O1001 的信息"
);
```

- `ignored` 表示“会传入一个 AgentEvalCase，但这次用不到”。
- `->` 右边是函数返回值。

2. 用真实 Agent 回答：

```java
runner.run(
    List.of(orderCase),
    evalCase -> orderAgent.chat(evalCase.input())
);
```

- `evalCase` 是函数入参；
- `evalCase.input()` 取出用例输入；
- `orderAgent.chat(...)` 用真实 Agent 生成答案。

记法：`Function<A, B>` 是“把 A 转成 B 的小机器”，`->` 左边是入口，右边是处理逻辑，`apply(...)` 是按下开关。

## 四、学习例子 + 企业例子

### 学习例子：弱回答失败，改进后通过

`EvalRegressionRunnerTest` 先用「订单 O1001 的信息」这种缺少 399 的弱回答，跑出失败；再换成包含 399 的回答，跑出通过。

### 企业例子：客服订单查询失败复盘

`FailureReviewLiveTest` 用真实 DeepSeek 完成同样的复盘链路：

```text
[复盘-失败报告] EvalRunReport[
  total=1, passed=0, failed=1,
  failures=[EvalFailure[caseId=ORDER_QUERY,
    actualAnswer=订单 O1001 的信息,
    reason=未通过：缺少检查点 [399]]]
]

[复盘-改进报告] EvalRunReport[total=1, passed=1, failed=0, failures=[]]
```

企业场景：客服查订单时，如果回答缺少金额，就是一次生产事故；评估要能自动抓住它，复盘中换回真实订单 Agent 后再回归通过。

## 五、如何本地测试

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent

# 1) 不调大模型，验证失败复盘逻辑
mvn test -Dtest=EvalRegressionRunnerTest

# 2) 真实 DeepSeek 联调：失败 → 复盘 → 真实 Agent 修复
.\scripts\test-live.ps1 -Test FailureReviewLiveTest
```

## 六、Day 6 完成标准

- [x] 能自动收集失败用例与失败原因
- [x] 形成失败 → 复盘 → 改进 → 回归的闭环
- [x] 有学习例子和企业例子，并真实调用大模型
