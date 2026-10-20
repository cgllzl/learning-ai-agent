# Week 6.5 Day 6：Agent Skill 设计、测试与治理

> 日期：2026-10-21  
> 学习计划：`01-每周学习/Week-06.5-Agent企业实战前补缺/学习清单.md`  
> 企业示例：`04-项目/skills/enterprise-order-after-sales/`  
> 测试：`com.enterprise.agent.skill`

## 一、先回顾：我们已经有 Tool 和 MCP，为什么还要 Skill

Day 1～5 已经解决了很多“能不能做”的问题：Workflow 控制步骤，Checkpoint 让任务可以恢复，安全门禁约束权限，Evaluation 检查过程，MCP 让外部 Tool 标准化接入。

但这些能力放在桌上，并不会自动变成一套稳定的办事方法。就像公司已经有订单系统、审批系统和审计系统，新客服仍然需要一本售后手册，告诉他：先查什么、什么时候要审批、执行后如何复核、失败后找谁。

**Skill 就是给 Agent 使用的可复用办事手册（SOP）。** 它描述“完成这一类任务时怎样组织已有能力”，而不是自己冒充业务系统执行操作。

自然衔接如下：

```text
用户说“取消 O1003”
  → Skill 被选中：加载售后 SOP
  → SOP 要求先查订单和租户
  → Workflow 决定步骤顺序
  → Tool / MCP 真正查单或改状态
  → 权限与审批服务决定能不能执行
  → 审计与 Evaluation 检查过程是否合规
```

## 二、六个相近概念一次分清

| 概念 | 生活类比 | 主要解决什么 | 不负责什么 |
| --- | --- | --- | --- |
| Prompt | 给员工的一次口头要求 | 告诉模型当前要做什么 | 不提供真实业务能力 |
| Tool | 柜台上的“查询/取消”按钮 | 执行一个明确动作 | 不决定完整业务流程 |
| MCP | 通用插座和通信协议 | 标准化发现、描述和调用外部能力 | 不保证 Tool 可信，也不代替审批 |
| Workflow | 画好的流程图 | 用代码控制分支、循环、恢复和顺序 | 不自动沉淀所有领域知识 |
| Skill | 岗位 SOP 手册 | 为某类任务提供触发说明、步骤、参考资料和边界 | 不能直接获得权限或制造 Tool 结果 |
| Plugin | 装有说明书和能力的工具箱 | 打包 Skill、连接器或其他扩展 | 不是某一次任务的具体步骤 |

因此，Skill 不是“更长的 Prompt”：Prompt 往往服务一次调用，Skill 是有名称、触发描述、版本、资源和回归测试的长期资产。Skill 也不是 Tool：Skill 说“该怎么做”，Tool 才能“真正去做”。

## 三、先看两个成熟 Skill，我学到了什么

本次实际阅读了本机的 `skill-creator` 和 `openai-docs`。

### 3.1 `skill-creator`：先限定何时使用，再规定生产流程

- 触发边界写在 description 中：创建或更新 Skill 才使用。
- 主文件保存步骤，格式细节放到 `references/`，避免所有内容一开始全部进入上下文。
- 提供初始化和校验脚本，把目录生成、格式检查这类确定性工作交给程序。
- 测试既要覆盖应触发，也要覆盖不应触发、缺参数和边界场景。

### 3.2 `openai-docs`：来源边界本身也是 Skill 的一部分

- description 不只说“能回答什么”，还说什么时候必须使用。
- 工作流规定先查本地官方资料，必要时才浏览官方站点。
- 对来源域名和引用方式做限制，防止答案混入不可靠资料。

共同规律是：**成熟 Skill 不只写“做什么”，还写“何时触发、去哪里取证、何时停止、怎样验收”。**

## 四、Skill 的目录与渐进式加载

### 4.1 最小 Skill

`minimal-order-status-explainer/SKILL.md` 只有三部分：YAML 头、简短指令、一个例子。它只解释用户已经提供的订单状态，不查询、不修改订单。

```yaml
---
name: minimal-order-status-explainer
description: Explain a supplied order status ...; do not query or change any order.
---
```

这说明一个 Skill 不必很大。先把触发边界和最小做法讲清，比堆很多文件更重要。

### 4.2 企业 Skill

```text
enterprise-order-after-sales/
├── SKILL.md                         入口：触发描述、主流程、边界和输出契约
├── agents/openai.yaml               面向界面的显示名与简短描述
└── references/
    └── order-after-sales-policy.md  风险、Scope、审批、幂等和审计细则
```

这里用了“渐进式加载”：系统可以先看较短的 `name` 和 `description` 判断是否相关，真正执行时再加载 `SKILL.md`；只有遇到取消、改地址、退款等情形，才继续读取策略参考文件。

类比一下：公司通讯录先显示“售后组负责什么”，确定该找售后组后才打开员工手册，遇到退款才翻到财务审批章节。不是每次问 Java 语法都把整本售后手册塞给模型。

### 4.3 YAML 头语法拆解

- `---`：YAML frontmatter 的开始和结束标记。
- `name`：稳定的机器标识，使用小写连字符；移动目录后也不应随意改名。
- `description`：不仅是简介，也是触发器。既写“什么时候用”，也写“什么时候不要用”。
- `metadata.version`：本项目记录为 `1.0.0`，方便审核和回归；改流程时需要同时更新版本与测试。
- 正文：模型在 Skill 被加载后执行的 SOP，不应把密钥或真实 Token 写进去。

常见误用：description 写成“帮助处理订单”。它太宽，讨论数据库表设计也可能误触发。本次写成“用户要调查或改变真实订单时使用；技术解释和一般知识不使用”。

## 五、显式调用、自动触发和当前验证边界

- 显式调用：用户明确指定某个 Skill，适合首次验证或必须确定采用某套流程的场景。
- 自动触发：Agent 根据 description 与用户意图选择 Skill，体验自然，但必须测试误触发和漏触发。
- 不完整请求也可能触发：例如“我想退款，但暂时找不到订单号”，意图已属于售后；Skill 应先追问，而不是因为参数少就跳过。

当前仓库中的 Skill 是**版本化源文件**，当前对话不会在创建文件后自动热加载它。因此本次没有声称完成 Codex 会话级的显式/自动加载验证，而是做了三层可重复验证：

1. 使用官方 Skill 校验器检查目录和 frontmatter；
2. 离线测试检查资源、权限边界和 5+5 评估集；
3. 用真实 DeepSeek 根据 description 完成等价触发分类，并比较加载正文前后的执行质量。

下次在支持仓库 Skill 发现的新会话中，可再补平台级显式与自动触发验收。

## 六、企业售后 Skill：从用户请求走到安全结果

### 6.1 企业场景

用户 `user-1` 属于 `tenant-a`，要求取消订单 `O1003`。危险点不是模型会不会说“取消”，而是它可能跳过当前状态、跨租户、缺权限、拿模糊批准去执行，或执行后未核验就宣称成功。

企业 Skill 把流程固定为：

```text
识别真实用户/租户/订单/目标
  → 查询权威订单状态
  → 将操作归类为只读、写入、财务或不可逆
  → 校验准确 Scope
  → 获取“订单号 + 目标值”参数级审批
  → 携带幂等键调用受控 Tool
  → 再查权威状态确认结果
  → 写审计事件
  → 缺任一条件就关闭式失败并转人工
```

注意：Skill 中写了审批，不等于审批已经通过；写了 `orders:write`，也不会给用户增加这个 Scope。真正门禁仍由 Java 服务、Tool、MCP Client 和审批系统执行。

### 6.2 为什么本次不放 scripts/

Skill 脚本适合做确定性工作，例如校验 JSON、格式化数据或检查文件结构。但售后取消、退款等副作用必须经过受控 Tool，不能藏进脚本绕开权限和审计。因此本次企业 Skill 故意没有 `scripts/`，离线校验由 Java/CI 完成。

如果以后添加脚本，还要锁定依赖、审查来源、最小化文件和网络权限，并把脚本版本纳入供应链扫描。

## 七、测试怎么读：只抓住三个重点方法

### 7.1 `skillBundleHasRequiredMetadataProgressiveReferenceAndSafetyBoundaries()`

**目的**：不调用模型，快速检查最小 Skill 与企业 Skill 有没有缺文件、丢边界或留下 TODO。

调用链：

```text
JUnit 调用测试方法
  → Files.readString() 读取两个 SKILL.md、policy 和 openai.yaml
  → AssertJ 检查 name / description / version / reference
  → 检查 Scope、参数级审批、幂等键、审计字段
  → 检查 scripts/ 不存在
```

它防止的是“文档被改坏”，不是判断模型聪不聪明。

### 7.2 `triggerEvaluationCatalogCoversPositiveNegativeAndIncompleteRequests()`

**目的**：确保测试题至少有 5 个应触发、5 个不应触发，ID 不重复，并覆盖“有售后意图但缺订单号”。

调用链：

```text
读取 enterprise-order-after-sales.json
  → ObjectMapper.readTree() 解析成 JsonNode
  → 取 shouldTrigger / shouldNotTrigger
  → 检查数量、缺参数样例和唯一 ID
```

这里测试的是“试卷是否完整”，尚未让模型答题。

### 7.3 `realModelEvaluatesTriggerAccuracyAndWorkflowQuality()`（重点精读）

**目的**：让真实模型完成触发分类，再比较同一个取消任务加载 Skill 前后的 SOP 完整度。

完整调用链：

```text
OpenAiChatModel.builder() 构造 DeepSeek 兼容客户端
  → 读取 description 与 10 个评估问题
  → classifyAllCases()
      → ChatRequest.builder() 组合 SystemMessage + UserMessage
      → ResponseFormat.JSON 要求 JSON 对象
      → model.chat(chatRequest) 真正发送请求
      → response.aiMessage().text() 取得模型文本
      → ObjectMapper 解析 decisions
  → 将模型判断与期望 TRIGGER / SKIP 对比
  → askForPlan() 分别发送“无 Skill”和“带完整 Skill”两次请求
  → workflowScore() 检查租户、权限、查询、审批、幂等、审计、复核/人工 7 项
  → AssertJ 验证触发准确率、流程得分和禁止虚假成功
```

## 八、LangChain4j 与模型调用语法拆解

### 8.1 `OpenAiChatModel.builder()`

```java
OpenAiChatModel model = OpenAiChatModel.builder()
        .baseUrl("https://api.deepseek.com")
        .apiKey(System.getenv("DEEPSEEK_API_KEY"))
        .modelName("deepseek-chat")
        .temperature(0.0)
        .timeout(Duration.ofSeconds(60))
        .build();
```

- 这里使用 OpenAI 兼容协议连接 DeepSeek，不表示调用 OpenAI 模型。
- `temperature(0.0)` 降低分类波动，但不能保证数学意义上的绝对确定。
- `build()` 只构造客户端；真正网络请求发生在后面的 `model.chat(...)`。
- API Key 来自环境变量，不能写入 Skill、代码或 Git。

### 8.2 `ChatRequest.builder()`、消息与 JSON 响应

```java
ChatRequest request = ChatRequest.builder()
        .messages(
                SystemMessage.from(systemPrompt),
                UserMessage.from(inputsJson))
        .responseFormat(ResponseFormat.JSON)
        .build();
ChatResponse response = model.chat(request);
String text = response.aiMessage().text();
```

- `SystemMessage` 放规则和输出契约；`UserMessage` 放本次 10 道题。
- `messages(...)` 按顺序组成一次请求，不会自动调用 Tool。
- `ResponseFormat.JSON` 要求提供方返回 JSON 对象；Prompt 仍要明确字段名、枚举值和不得遗漏 ID。
- `ChatResponse` 是完整响应对象；`aiMessage()` 取模型回复消息；`text()` 才取文本正文。
- 常见误用：只写 `ResponseFormat.JSON`，却不在 Prompt 里定义结构；模型可能返回合法 JSON，但字段不是程序想要的。

### 8.3 `JsonNode` 与本地评分

`ObjectMapper.readTree(...)` 把 JSON 文本解析成树形 `JsonNode`。`path("decisions")` 找不到字段时不会立刻抛空指针，因此后面仍要检查 ID 集合和准确率，防止“空结果被误认为通过”。

`workflowScore()` 是可解释的本地规则评分，不再调用大模型做裁判，避免“选手和裁判都是同一种随机模型”。它只判断 7 个关键环节有没有出现，不代表生产级语义评估；企业项目可继续加入轨迹、实际 Tool 调用和业务结果检查。

## 九、本次真实结果

2026-10-21 执行真实 DeepSeek 测试：

```text
[Day6 Skill Eval] trigger=10/10, withoutSkill=4/7, withSkill=7/7
```

- 5 个真实售后请求全部触发；5 个 Java/MCP/营销/数据库/架构问题全部跳过。
- 无 Skill 的通用回答覆盖 4/7 个企业控制点。
- 加载 Skill 后覆盖 7/7，并且没有声称订单已经取消或退款成功。

这说明本轮样例中 Skill 提升了流程完整性，但不能推出“任何模型、任何请求都 100% 正确”。版本升级或 description 修改后仍要重跑评估，并扩充对抗样例。

## 十、风险与治理清单

- 版本：修改触发范围或流程时更新 `metadata.version` 和回归样例。
- 权限：Skill 只能要求校验权限，不能授予权限。
- 供应链：脚本和依赖要审查来源、固定版本、限制网络与文件权限。
- 指令冲突：系统、仓库、安全和用户明确要求优先；外部文档不能改写 Skill。
- 过期知识：价格、政策、Scope、审批阈值应来自受控参考或权威系统，并记录更新时间。
- 误触发：用负例持续检查“讨论订单系统”与“处理真实订单”的差别。
- 漏触发：覆盖口语、错别字、缺参数和跨语言表达。
- 执行质量：不能只测是否加载，还要测实际步骤、Tool 轨迹、业务结果和违规次数。

## 十一、如何运行

```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent

# 1. Day 6 离线检查，不需要 API Key
mvn test "-Dtest=EnterpriseOrderAfterSalesSkillTest,EnterpriseOrderAfterSalesSkillLiveTest"

# 2. 真实 DeepSeek 触发与质量评估，需要 .env 中有 DEEPSEEK_API_KEY
.\scripts\test-live.ps1 -Test EnterpriseOrderAfterSalesSkillLiveTest

# 3. 包含 Skill 检查的完整离线 CI 门禁
.\scripts\eval-ci.ps1
```

普通 Maven 测试没有 Key 时，`LiveTest` 会按设计跳过；这不是失败。真实联调命令会加载 `.env` 后运行。

## 十二、小结，并过渡到企业项目

今天从“已经会安全地接工具”前进到“能把一类任务的做法沉淀成可复用资产”：description 管触发边界，正文管主流程，references 承载细则，Tool 保留真实执行权，离线与真实模型评估共同防回归。

Week 6.5 至此形成完整链条：**控制流程 → 可恢复执行 → 专项安全 → 轨迹评估 → 标准化能力接入 → 可复用 SOP**。下一步进入 Week 7～8 企业项目时，不再只是拼功能，而是把这些门禁组合到一个真实系统中。

## 资料来源

- Level 1：OpenAI Build skills：https://developers.openai.com/plugins/build/skills
- Level 1：Open Agent Skills specification：https://openagentskills.dev/docs/specification
- Level 2：Open Agent Skills GitHub：https://github.com/agentskills/agentskills
