package com.enterprise.agent.prompt;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.List;

/**
 * Prompt 工程示例服务：System Prompt、Few-shot、Chain-of-Thought、企业工单分诊。
 */
public class PromptEngineeringService {

    private static final String FEW_SHOT_SYSTEM = """
            你是客服工单分类器。只输出一个类别词：物流、退款、售后、咨询。
            示例：
            输入：我的快递到哪了？ -> 物流
            输入：我要退款 -> 退款
            输入：键盘坏了 -> 售后
            输入：这个商品多少钱？ -> 咨询""";

    private static final String COT_SYSTEM = """
            你是计算助手。请逐步思考并给出最终答案。
            第一步：列出已知条件；第二步：分步计算；第三步：只输出最终数字。""";

    private static final String TICKET_SYSTEM = """
            你是企业客服中心的分诊员。请判断客户问题应该转给哪个部门，只输出一个词：售前、售后、物流。
            规则：
            - 咨询产品、价格、库存 -> 售前
            - 退货、退款、投诉、维修 -> 售后
            - 快递、发货、收货地址 -> 物流""";

    private final OpenAiChatModel chatModel;

    public PromptEngineeringService(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String fewShotClassify(String input) {
        return chat(FEW_SHOT_SYSTEM, input);
    }

    public String chainOfThought(String question) {
        return chat(COT_SYSTEM, question);
    }

    public String enterpriseTicketClassify(String input) {
        return chat(TICKET_SYSTEM, input);
    }

    private String chat(String systemPrompt, String userMessage) {
        ChatResponse response = chatModel.chat(List.of(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userMessage)));
        return response.aiMessage().text();
    }
}
