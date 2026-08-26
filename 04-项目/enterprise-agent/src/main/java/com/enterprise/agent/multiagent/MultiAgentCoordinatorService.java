package com.enterprise.agent.multiagent;

import com.enterprise.agent.agent.OrderAgentService;
import org.springframework.stereotype.Service;

/**
 * 多 Agent 串行编排器：演示「状态传递 + 结果合并」。
 *
 * 流程：
 * 1. 订单查询 Agent（真实工具 + 大模型）先回答问题，产出一段「订单事实」作为共享状态。
 * 2. 客服回复 Agent（另一个大模型）接收这段状态，合并润色成最终答案。
 */
@Service
public class MultiAgentCoordinatorService {

    private final OrderAgentService orderAgentService;
    private final CustomerReplyService customerReplyService;

    public MultiAgentCoordinatorService(OrderAgentService orderAgentService,
                                        CustomerReplyService customerReplyService) {
        this.orderAgentService = orderAgentService;
        this.customerReplyService = customerReplyService;
    }

    public String handleCustomerQuestion(String question) {
        // 第一步：订单查询 Agent 产出状态
        String orderFacts = orderAgentService.chat(question);

        // 第二步：把状态传给客服回复 Agent，完成结果合并
        return customerReplyService.compose(question, orderFacts);
    }
}
