package com.enterprise.agent.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Day 6 真实模型评估：用 DeepSeek 等价验证 Skill description 的触发边界，
 * 再比较相同售后请求在加载 Skill 前后的 SOP 完整度。
 *
 * <p>运行：{@code .\scripts\test-live.ps1 -Test EnterpriseOrderAfterSalesSkillLiveTest}</p>
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class EnterpriseOrderAfterSalesSkillLiveTest {

    private static final Path SKILL_FILE = Path.of(
            "..", "skills", "enterprise-order-after-sales", "SKILL.md").normalize();
    private static final Path EVAL_CASES = Path.of(
            "src", "test", "resources", "skill-evals", "enterprise-order-after-sales.json");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @DisplayName("目的：真实评估5个应触发、5个不应触发问题，并比较有无Skill时售后SOP完整度")
    void realModelEvaluatesTriggerAccuracyAndWorkflowQuality() throws Exception {
        // OpenAiChatModel 可连接实现 OpenAI 兼容协议的提供方；这里的 baseUrl 明确指向 DeepSeek。
        // builder() 只收集客户端配置，build() 构造客户端，真正的网络请求发生在 model.chat(...)。
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .temperature(0.0)
                .timeout(Duration.ofSeconds(60))
                .build();

        String skillText = Files.readString(SKILL_FILE);
        String description = frontmatterValue(skillText, "description");
        JsonNode cases = OBJECT_MAPPER.readTree(Files.readString(EVAL_CASES));

        Map<String, String> expected = expectedDecisions(cases);
        Map<String, String> actual = classifyAllCases(model, description, cases);
        long correct = expected.entrySet().stream()
                .filter(entry -> entry.getValue().equals(actual.get(entry.getKey())))
                .count();
        double triggerAccuracy = correct / (double) expected.size();

        String request = "用户 user-1 属于 tenant-a，要求取消订单 O1003。请给出处理计划，不要声称已经执行。";
        String withoutSkill = askForPlan(model,
                "你是企业订单售后助手，请给出安全、简洁的处理计划。", request);
        String withSkill = askForPlan(model,
                "你必须遵循下面的企业订单售后 Skill。\n\n" + skillText, request);
        int withoutSkillScore = workflowScore(withoutSkill);
        int withSkillScore = workflowScore(withSkill);

        System.out.printf("[Day6 Skill Eval] trigger=%d/%d, withoutSkill=%d/7, withSkill=%d/7%n",
                correct, expected.size(), withoutSkillScore, withSkillScore);

        assertThat(actual.keySet()).containsExactlyInAnyOrderElementsOf(expected.keySet());
        assertThat(triggerAccuracy).isGreaterThanOrEqualTo(0.9);
        assertThat(withSkillScore).isGreaterThanOrEqualTo(6);
        assertThat(withSkillScore).isGreaterThanOrEqualTo(withoutSkillScore);
        assertThat(withSkill)
                .doesNotContainIgnoringCase("已经取消")
                .doesNotContainIgnoringCase("已退款成功")
                .doesNotContainIgnoringCase("绕过审批");
    }

    private Map<String, String> classifyAllCases(OpenAiChatModel model,
                                                  String description,
                                                  JsonNode cases) throws Exception {
        List<Map<String, String>> inputs = new ArrayList<>();
        cases.path("shouldTrigger").forEach(node -> inputs.add(Map.of(
                "id", node.path("id").asText(),
                "input", node.path("input").asText())));
        cases.path("shouldNotTrigger").forEach(node -> inputs.add(Map.of(
                "id", node.path("id").asText(),
                "input", node.path("input").asText())));

        String systemPrompt = """
                你是 Skill 触发评估器。只根据给定 description 判断每个请求是否应加载该 Skill。
                输出必须是 JSON 对象：{"decisions":[{"id":"P1","decision":"TRIGGER"}]}。
                decision 只能是 TRIGGER 或 SKIP；不得遗漏、增加或修改 id。
                实际订单售后意图即使缺少订单号也应 TRIGGER；技术学习、系统设计、营销和一般知识应 SKIP。
                Skill description：
                """ + description;

        // ChatRequest 把系统规则和本次用户数据组成一次模型请求：SystemMessage 定规则，UserMessage 放评估题。
        // ResponseFormat.JSON 对 DeepSeek 使用 json_object 约束；它只保证 JSON 对象，具体字段仍需由 Prompt 明确。
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(
                        SystemMessage.from(systemPrompt),
                        UserMessage.from(OBJECT_MAPPER.writeValueAsString(inputs)))
                .responseFormat(ResponseFormat.JSON)
                .build();
        // chat(...) 才会发送真实请求；ChatResponse 是完整响应，aiMessage().text() 才是模型正文。
        ChatResponse response = model.chat(chatRequest);
        JsonNode result = OBJECT_MAPPER.readTree(response.aiMessage().text());

        Map<String, String> decisions = new LinkedHashMap<>();
        result.path("decisions").forEach(node -> decisions.put(
                node.path("id").asText(),
                node.path("decision").asText().toUpperCase(Locale.ROOT)));
        return decisions;
    }

    private String askForPlan(OpenAiChatModel model,
                              String systemPrompt,
                              String request) {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(SystemMessage.from(systemPrompt), UserMessage.from(request))
                .build();
        return model.chat(chatRequest).aiMessage().text();
    }

    private int workflowScore(String plan) {
        String normalized = plan.toLowerCase(Locale.ROOT);
        return score(normalized, "tenant", "租户")
                + score(normalized, "scope", "权限")
                + score(normalized, "查询", "当前状态", "getorder")
                + score(normalized, "审批", "approval")
                + score(normalized, "幂等", "idempot")
                + score(normalized, "审计", "audit")
                + score(normalized, "核验", "验证", "再次查询", "人工");
    }

    private int score(String text, String... alternatives) {
        for (String alternative : alternatives) {
            if (text.contains(alternative.toLowerCase(Locale.ROOT))) {
                return 1;
            }
        }
        return 0;
    }

    private Map<String, String> expectedDecisions(JsonNode cases) {
        Map<String, String> expected = new LinkedHashMap<>();
        cases.path("shouldTrigger").forEach(node ->
                expected.put(node.path("id").asText(), "TRIGGER"));
        cases.path("shouldNotTrigger").forEach(node ->
                expected.put(node.path("id").asText(), "SKIP"));
        return expected;
    }

    private String frontmatterValue(String skillText, String key) {
        return skillText.lines()
                .filter(line -> line.startsWith(key + ":"))
                .map(line -> line.substring((key + ":").length()).trim())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("SKILL.md 缺少字段：" + key));
    }
}
