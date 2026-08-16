# Day 3：企业订单 Agent（查询订单）

> Week 2 ｜ 归档：`05-记录/归档/2026-08-16-Week2-Day3-订单查询Agent.md` ｜ 代码：`com.enterprise.agent.agent`

## 一、目标

把 Day 2 的工具能力通过 HTTP 接口暴露出来，让外部（Apifox / 前端）可以自然语言查询订单。

## 二、三件套回顾 + 新增接口

- Day 2 已完成：`OrderTools.getOrder`（@Tool）+ `AiServices` 注册（`OrderAgentService`）。
- Day 3 新增：`OrderAgentController` 暴露 `POST /agent/order`。

```java
@RestController
@RequestMapping("/agent")
public class OrderAgentController {

    private final OrderAgentService orderAgentService;

    @PostMapping("/order")
    public ChatResponse order(@Valid @RequestBody OrderAgentRequest request) {
        return new ChatResponse(orderAgentService.chat(request.message()));
    }
}
```

## 三、接口文档

```http
POST /agent/order
Content-Type: application/json

{ "message": "查询订单 O1001 的信息" }
```

```json
{ "reply": "订单 O1001：用户 U1，商品 P1，金额 399.0 元，状态 PAID" }
```

- 入参：`message`（必填，@NotBlank 校验，为空返回 400）
- 错误码：400 参数错误 / 503 AI 服务不可用 / 502 其他异常（`OrderAgentExceptionHandler`）

## 四、一次完整调用（内部发生了什么）

1. Apifox 发 `POST /agent/order`，Body 是自然语言
2. `OrderAgentController` → `OrderAgentService.chat(...)` → 代理（AiServices）组装消息 + 工具列表
3. DeepSeek 返回 `tool_calls { getOrder, {orderId: "O1001"} }`
4. 代理执行 `OrderTools.getOrder("O1001")` → 真实数据
5. 结果回填 → 模型给出最终回答 → 返回给 Apifox

> 完整时序见 `Day2-第一个Java-Tool.md` 的调用时序图。

## 五、验证

- `OrderAgentControllerTest`：接口返回 reply、空 message 返回 400
- `OrderAgentLiveTest`（真实 DeepSeek）：问「查询订单 O1001」，回复含「399」（证明工具被真实调用）

## 六、Day 3 完成标准

- [x] `POST /agent/order` 可用（真实联调通过）
- [x] 自然语言提问能触发正确 Tool