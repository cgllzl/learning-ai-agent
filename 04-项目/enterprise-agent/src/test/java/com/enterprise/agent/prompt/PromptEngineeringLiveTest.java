package com.enterprise.agent.prompt;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prompt 工程的大模型例子。
 * 学习例子：Few-shot 分类 + Chain-of-Thought。
 * 企业例子：客服中心工单分诊。
 * 需设置 DEEPSEEK_API_KEY：
 *   .\scripts\test-live.ps1 -Test PromptEngineeringLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class PromptEngineeringLiveTest {

    @Test
    void promptTechniquesWorkForLearningAndEnterpriseCases() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .build();

        PromptEngineeringService service = new PromptEngineeringService(model);

        String fewShotReply = service.fewShotClassify("我的快递到哪了？");
        System.out.println("[Few-shot 分类] " + fewShotReply);
        assertThat(fewShotReply).contains("物流");

        String cotReply = service.chainOfThought("商品原价200元，会员打8折，再减20元，最终多少钱？");
        System.out.println("[CoT 计算] " + cotReply);
        assertThat(cotReply).contains("140");

        String enterpriseReply = service.enterpriseTicketClassify("我买的机械键盘坏了，要求退货退款");
        System.out.println("[企业工单分诊] " + enterpriseReply);
        assertThat(enterpriseReply).contains("售后");
    }
}
