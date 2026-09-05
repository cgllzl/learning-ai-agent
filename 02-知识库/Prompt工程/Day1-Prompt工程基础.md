# Day 1：Prompt 工程基础（补全 Week 1 遗留知识）

> 补全日期：2026-09-05 ｜ 代码：`com.enterprise.agent.prompt.PromptEngineeringService`

## 一、先讲人话：Prompt 是什么

Prompt 就是「你给大模型的话」。它不像普通聊天那么随意，因为大模型对措辞、顺序、示例都很敏感。Prompt 工程的目标是：**用尽量少的输入，得到稳定、可控的输出**。

## 二、四个必须掌握的技术

| 技术 | 一句话解释 | 适用场景 |
| --- | --- | --- |
| System / User Prompt 分工 | System 定角色规则，User 给本次任务 | 所有 Agent |
| Few-shot | 给几个输入→输出示例 | 分类、格式统一 |
| Chain-of-Thought | 让模型“先想步骤，再给答案” | 计算、推理 |
| 结构化输出指令 | 明确字段、格式、约束 | 稳定 JSON / 分类 |

## 三、代码示例

`PromptEngineeringService` 用三段 Prompt 演示三种技术：

```java
// 1) Few-shot：给模型几个例子，统一输出格式
public String fewShotClassify(String input) {
    return chat(FEW_SHOT_SYSTEM, input);
}

// 2) Chain-of-Thought：要求分步思考
public String chainOfThought(String question) {
    return chat(COT_SYSTEM, question);
}

// 3) 企业工单分诊：把自然语言转成固定分类
public String enterpriseTicketClassify(String input) {
    return chat(TICKET_SYSTEM, input);
}
```

## 四、学习例子 + 企业例子

`PromptEngineeringLiveTest` 用真实 DeepSeek 验证：

- 学习例子：Few-shot 分类“快递到哪了”→ 物流；CoT 计算 200×0.8−20 → 140。
- 企业例子：客服工单“键盘坏了，要求退货退款”→ 售后。

真实输出：

```text
[Few-shot 分类] 物流
[CoT 计算] ... 最终数字：140
[企业工单分诊] 售后
```

企业场景：客服中心每天收到大量工单，先用 Prompt 分诊到售前/售后/物流，能减少人工路由成本。

## 五、如何本地测试

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent
.\scripts\test-live.ps1 -Test PromptEngineeringLiveTest
```

## 六、完成标准

- [x] Prompt 工程目录不再只有 README
- [x] 有 Few-shot / CoT / 企业工单分诊的代码例子
- [x] 有真实调用大模型的学习例子与企业例子
