package com.enterprise.agent.supervisor;

import com.enterprise.agent.agent.OrderAgentService;
import com.enterprise.agent.rag.RagQaService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * Supervisor 的"工具箱"：每个子助手被包装成一个 @Tool。
 * 总调度 Agent 根据用户问题，选择调用哪一个子助手。
 */
@Component
public class SupervisorTools {

    private final OrderAgentService orderAgentService;
    private final RagQaService ragQaService;

    public SupervisorTools(OrderAgentService orderAgentService, RagQaService ragQaService) {
        this.orderAgentService = orderAgentService;
        this.ragQaService = ragQaService;
    }

    @Tool("处理订单、用户、商品、物流、修改订单状态等业务问题")
    public String handleOrder(@P("用户的业务问题") String question) {
        return orderAgentService.chat(question);
    }

    @Tool("处理企业内部知识库、制度文档类问题")
    public String handleKnowledge(@P("用户的知识问题") String question) {
        return ragQaService.ask(question, null, 5).answer();
    }
}