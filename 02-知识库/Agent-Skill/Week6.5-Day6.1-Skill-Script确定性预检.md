# Day 6.1：Skill Script，给售后请求配一张“材料核对表”

> 日期：2026-10-22
> 对应计划：`01-每周学习/第二阶段-Agent企业实战/Day6.1-Skill-Script学习清单.md`
> 代码：`04-项目/skills/enterprise-order-after-sales/scripts/validate_after_sales_request.py`
> 版本：企业售后 Skill `1.0.0 → 1.1.0`

## 一、回顾 → 为什么自然引出 Script

Day 6 已经知道 Skill 像岗位 SOP：收到取消订单请求，先查订单，再过权限与审批，执行后复核。然而“订单号有没有填、退款金额是不是正数、缺哪个字段”这类判断固定、重复。如果每次都让模型临场判断，答案可能不一致。

于是给 SOP 配一张机器可读的**材料核对表**：模型负责听懂用户说的是“取消订单”；Script 负责按固定规则检查候选 JSON。核对表说“材料齐全”，不代表工作人员已经验明身份，更不代表订单已经取消。

根据 [OpenAI Build skills](https://developers.openai.com/plugins/build/skills)，`scripts/` 适合确定性计算和文件处理，只有能明显提高可靠性时才添加；这就是本次选择它的原因。

## 二、最小学习例子：查订单

给脚本输入：

```json
{"userId":"user-1","tenantId":"tenant-a","orderId":"O1003","action":"QUERY"}
```

得到：

```json
{"valid":true,"riskLevel":"READ_ONLY","requiredScope":"orders:read","missingFields":[],"invalidFields":[],"nextStep":"RUNTIME_AUTHORIZATION"}
```

`valid: true` 只说明字段形状符合要求。下一站仍是 Java 运行时校验：这个用户是否真属于该租户、是否拥有 `orders:read`、是否能看这张订单。脚本不能自己回答这些问题。

## 三、企业例子：取消 O1003

真实 DeepSeek 从“我想取消订单 O1003”提取：

```json
{"action":"CANCEL","orderId":"O1003"}
```

脚本给出：

```json
{"valid":false,"riskLevel":"MUTATING","requiredScope":"orders:write","missingFields":["userId","tenantId","approvalId","idempotencyKey"],"invalidFields":[],"nextStep":"NEED_TRUSTED_CONTEXT"}
```

调用链像这样：

```text
用户说“我想取消订单 O1003”
  → DeepSeek 提取动作 CANCEL、订单号 O1003
  → Java LiveTest 用 ProcessBuilder 启动 Python 脚本
  → 将模型 JSON 写入脚本的标准输入 stdin
  → 脚本按固定规则输出缺失字段
  → Java 断言没有进入 Tool，下一步是取得受信身份与租户
  → 后续业务服务再办理审批、幂等、授权和审计
```

若候选 JSON 自己写了 `"scope":"orders:write"`，脚本也不会把它当作已经授权。`requiredScope` 只是告诉受信运行时“需要检查什么”。`approvalId` 同理，单有字符串不能证明审批真实、未过期、绑定了这次参数。

## 四、脚本如何工作：新语法逐步拆解

### 4.1 `json.loads` 与 `dict`

`json.loads(raw.decode("utf-8"))` 把标准输入的 UTF-8 字节解析为 Python 对象。本脚本只接受 JSON 对象，即 Python `dict`。例如 `{"action":"QUERY"}` 是字典，`["QUERY"]` 是列表，后者被拒绝。

`request.get("action")` 取字段；字段不存在时返回 `None`，不会直接抛异常。随后通过 `ACTION_RULES` 白名单决定风险与必填字段。未知动作如 `EXPORT_ALL` 返回 `REJECT_UNSUPPORTED_ACTION`，模型不能用新动作名绕过流程。

### 4.2 `sys.stdin.buffer.read` 与 `sys.stdout`

`stdin` 是进程的输入管道，`stdout` 是输出管道。Java 把 JSON 写进输入管道，脚本打印唯一一行 JSON 结果。脚本限制最多 64 KiB，避免异常大的候选输入。坏 JSON 返回退出码 2，业务字段缺失则正常退出并返回 `valid:false`，两种失败不混为一谈。

### 4.3 `Decimal` 与金额

退款金额用 `Decimal` 检查是否大于零，并拒绝 `NaN` 或无限值。金额不用二进制 `float` 做精确判断，是因为货币小数容易出现表示误差。这里仅做格式与正数检查，实际币种、金额上限、可退余额由财务服务校验。

### 4.4 `sys.exit(main())`

`main()` 返回 0 表示脚本正常完成，即使业务预检结果是 `valid:false`；返回 2 表示输入损坏或超长，脚本无法给出业务预检结果。这样上层能区分“请补资料”和“脚本自身出错”。

## 五、Java / LangChain4j 语法与返回值

真实模型测试中，`OpenAiChatModel.builder().baseUrl("https://api.deepseek.com")` 构造一个连接 DeepSeek 兼容接口的客户端；`build()` 不发送请求。此前 Day 6 已首次详细拆解 Builder、`ChatRequest`、`SystemMessage`、`UserMessage` 和 `ResponseFormat.JSON`，可回看 [Day 6 语法拆解](Week6.5-Day6-Agent-Skill设计测试与治理.md#八langchain4j-与模型调用语法拆解)。

本次新加入的 Java 写法是：

```java
Process process = new ProcessBuilder(command).start();
try (var stdin = process.getOutputStream()) {
    stdin.write(OBJECT_MAPPER.writeValueAsBytes(candidate));
}
String result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
```

- `ProcessBuilder(command).start()` 启动本地脚本进程；`command` 在 Windows 使用 `py -3`，在 Linux 使用 `python3`。
- `process.getOutputStream()` 是**Java 往子进程写**的管道，也就是 Python 的 `stdin`；名字容易看反。
- `writeValueAsBytes(candidate)` 把模型返回的 `JsonNode` 序列化为 JSON 字节。
- `try (...)` 退出代码块时关闭管道，Python 才知道输入结束。
- `process.getInputStream()` 是**Java 读取子进程输出**的管道，也就是 Python 的 `stdout`。
- `process.waitFor()` 得到退出码；再用 `ObjectMapper.readTree(result)` 解析脚本结果。它只代表预检结果，不是 Tool 执行结果。

常见误用：把 `valid:true` 直接映射成“立即调用写 Tool”。正确做法是让 Java 的受信身份、Scope、参数级审批、幂等、Tool 审核与审计门禁继续决定能否执行。

## 六、每个测试方法的目的

Python 的 `test_validate_after_sales_request.py` 共有 5 个行为测试：

| 方法 | 目的 |
| --- | --- |
| `test_query_needs_identity_and_then_requests_runtime_authorization` | 查单字段齐全后仍要求运行时鉴权 |
| `test_cancel_without_approval_or_idempotency_stops` | 取消订单缺审批与幂等键会停止，伪造 Scope 不起作用 |
| `test_refund_requires_positive_amount_currency_and_human_review` | 退款金额必须为正，字段齐全后仍转人工 |
| `test_unsupported_action_cannot_turn_into_tool_call` | 未知动作不能进入 Tool |
| `test_malformed_json_is_a_program_error` | 损坏 JSON 用非零退出码与业务拒绝区分 |

重点精读 Java `realModelCandidateIsCheckedByDeterministicScript()`。它实际请求 DeepSeek，再把模型候选 JSON 交给脚本；断言缺失字段和下一步状态。Day 6 原有的 `realModelEvaluatesTriggerAccuracyAndWorkflowQuality()` 继续验证 5+5 触发和有无 Skill 的流程完整度。

## 七、验证命令与本次结果

在 `F:\ChatGPT\学习之路\04-项目\enterprise-agent`：

```powershell
# 离线 Java 30 个主题测试 + Python 5 个脚本行为测试
.\scripts\eval-ci.ps1

# 真实 DeepSeek：触发评估 + 模型提取意图 → 脚本预检
.\scripts\test-live.ps1 -Test EnterpriseOrderAfterSalesSkillLiveTest
```

2026-10-22 两次实测：真实模型触发均为 10/10；无 Skill 的流程覆盖分别是 5/7、4/7，带 Skill 两次均为 7/7。第二个 LiveTest 提取 `CANCEL/O1003`，脚本发现缺用户、租户、审批、幂等键。无 Skill 得分有波动，说明单次模型输出不能当作稳定性结论。

全量 Java 回归另执行 231 个测试，0 失败、0 错误；37 个需要外部条件的 LiveTest 在普通环境按设计跳过。脚本的 5 个行为测试由 `eval-ci.ps1` 与 GitHub Actions 单独执行。

## 八、小结 → 过渡到第二阶段

今天给 Skill 加上了“小而硬”的确定性检查。模型负责理解，脚本负责格式预检，Java 业务服务负责真正授权和执行。下一步进入 [优先级 1：企业项目主链路整合](../../01-每周学习/第二阶段-Agent企业实战/优先级1-企业项目主链路整合.md)，把这些部件放进同一条订单请求中。

## 来源

- Level 1：OpenAI Build skills：https://developers.openai.com/plugins/build/skills
- Level 1：Open Agent Skills specification：https://openagentskills.dev/docs/specification
