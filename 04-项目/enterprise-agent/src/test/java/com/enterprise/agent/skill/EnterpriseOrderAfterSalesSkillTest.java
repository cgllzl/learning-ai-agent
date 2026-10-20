package com.enterprise.agent.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Day 6 离线验证只有两个主题：Skill bundle 是否完整，以及触发评估集是否覆盖正反边界。
 */
class EnterpriseOrderAfterSalesSkillTest {

    private static final Path MINIMAL_SKILL = Path.of(
            "..", "skills", "minimal-order-status-explainer", "SKILL.md").normalize();
    private static final Path SKILL_ROOT = Path.of(
            "..", "skills", "enterprise-order-after-sales").normalize();
    private static final Path EVAL_CASES = Path.of(
            "src", "test", "resources", "skill-evals", "enterprise-order-after-sales.json");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @DisplayName("目的：验证 SKILL.md 元数据、渐进式参考文件和权限边界完整且无 TODO")
    void skillBundleHasRequiredMetadataProgressiveReferenceAndSafetyBoundaries() throws Exception {
        String minimalSkill = Files.readString(MINIMAL_SKILL);
        String skill = Files.readString(SKILL_ROOT.resolve("SKILL.md"));
        String policy = Files.readString(
                SKILL_ROOT.resolve("references/order-after-sales-policy.md"));
        String openAiMetadata = Files.readString(
                SKILL_ROOT.resolve("agents/openai.yaml"));

        assertThat(minimalSkill)
                .contains("name: minimal-order-status-explainer")
                .contains("description:", "## Example")
                .contains("Never invent an order state")
                .doesNotContain("TODO");
        assertThat(skill)
                .startsWith("---")
                .contains("name: enterprise-order-after-sales")
                .contains("description:")
                .contains("metadata:")
                .contains("references/order-after-sales-policy.md")
                .contains("A Skill is not a Tool")
                .contains("cannot add scopes")
                .doesNotContain("TODO");
        assertThat(policy)
                .contains("orders:read", "orders:write", "orders:refund")
                .contains("参数级审批", "幂等键", "审计最小字段")
                .doesNotContain("TODO");
        assertThat(openAiMetadata)
                .contains("display_name: \"企业订单售后处理\"")
                .contains("short_description:");

        // 当前 Skill 不带 scripts/：确定性校验由 Java/CI 做，业务副作用必须继续走受控 Tool。
        assertThat(SKILL_ROOT.resolve("scripts")).doesNotExist();
    }

    @Test
    @DisplayName("目的：验证触发评估集至少包含5个应触发和5个不应触发问题且ID不重复")
    void triggerEvaluationCatalogCoversPositiveNegativeAndIncompleteRequests() throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(Files.readString(EVAL_CASES));
        JsonNode positives = root.path("shouldTrigger");
        JsonNode negatives = root.path("shouldNotTrigger");

        assertThat(positives.size()).isEqualTo(5);
        assertThat(negatives.size()).isEqualTo(5);
        assertThat(positives.toString()).contains("暂时找不到订单号");

        Set<String> ids = new HashSet<>();
        positives.forEach(node -> ids.add(node.path("id").asText()));
        negatives.forEach(node -> ids.add(node.path("id").asText()));
        assertThat(ids).hasSize(10);
    }
}
