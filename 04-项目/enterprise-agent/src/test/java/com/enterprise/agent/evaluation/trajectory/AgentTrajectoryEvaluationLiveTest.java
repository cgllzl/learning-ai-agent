package com.enterprise.agent.evaluation.trajectory;

import com.enterprise.agent.agent.MockOrderData;
import com.enterprise.agent.agent.OrderTools;
import com.enterprise.agent.observability.CostCalculator;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Week 6.5 Day 4：同一留出用例交错运行 Single-Agent / Multi-Agent 各 5 次，
 * 记录真实 Tool、Token、成本、延迟和 Handoff，再生成稳定性对比报告。
 *
 * 运行：.\scripts\test-live.ps1 -Test AgentTrajectoryEvaluationLiveTest
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class AgentTrajectoryEvaluationLiveTest {

    private static final int REPEATED_RUNS = 5;

    @Test
    void comparesSingleAndMultiAgentOnSameHoldoutCaseAcrossFiveRunsEach() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .temperature(0.0)
                .timeout(Duration.ofSeconds(60))
                .build();
        // 这里沿用项目 Week 6 的评估单价，只用于同批实验比较；价格变化时应更新并保留版本。
        CostCalculator costCalculator = new CostCalculator(0.27, 1.10);
        TrajectoryEvalCase holdout = TrajectoryEvalCaseCatalog
                .byId("HOLDOUT_ORDER_QUERY_O1002");
        TrajectoryEvaluator evaluator = new TrajectoryEvaluator();

        TrajectoryRecordingOrderAgentService singleAgent =
                new TrajectoryRecordingOrderAgentService(
                        model, new OrderTools(new MockOrderData()), costCalculator);
        TrajectoryRecordingOrderAgentService orderPartOfMulti =
                new TrajectoryRecordingOrderAgentService(
                        model, new OrderTools(new MockOrderData()), costCalculator);
        TrajectoryRecordingMultiAgentService multiAgent =
                new TrajectoryRecordingMultiAgentService(model, orderPartOfMulti, costCalculator);

        List<AgentRunSample> singleSamples = new ArrayList<>();
        List<AgentRunSample> multiSamples = new ArrayList<>();
        for (int run = 1; run <= REPEATED_RUNS; run++) {
            // 交错执行，避免“先跑完 A、网络变慢后再跑 B”造成明显时段偏差。
            // 每次都走“真实调用 → 三层判定 → 压缩成统计样本”，不会用另一遍调用补采指标。
            AgentTrajectory singleTrajectory = singleAgent.run(holdout.id(), holdout.input());
            TrajectoryEvaluationResult singleResult =
                    evaluator.evaluate(singleTrajectory, holdout.expectation());
            singleSamples.add(AgentRunSample.from("SINGLE", singleTrajectory, singleResult));

            AgentTrajectory multiTrajectory = multiAgent.run(holdout.id(), holdout.input());
            TrajectoryEvaluationResult multiResult =
                    evaluator.evaluate(multiTrajectory, holdout.expectation());
            multiSamples.add(AgentRunSample.from("MULTI", multiTrajectory, multiResult));

            System.out.println("[Day4 run=" + run + "] single="
                    + summarize(singleTrajectory, singleResult)
                    + " | multi=" + summarize(multiTrajectory, multiResult));
        }

        AgentComparisonReport report = new AgentComparisonService()
                .compare(singleSamples, multiSamples);
        System.out.println("[Day4 Single vs Multi] " + report);

        assertThat(report.singleAgent().runs()).isEqualTo(REPEATED_RUNS);
        assertThat(report.multiAgent().runs()).isEqualTo(REPEATED_RUNS);
        assertThat(report.singleAgent().successRate()).isGreaterThanOrEqualTo(0.8);
        assertThat(report.multiAgent().successRate()).isGreaterThanOrEqualTo(0.8);
        assertThat(report.singleAgent().averageTokens()).isPositive();
        assertThat(report.multiAgent().averageTokens()).isPositive();
        assertThat(report.singleAgent().averageCostUsd()).isPositive();
        assertThat(report.multiAgent().averageCostUsd()).isPositive();
        assertThat(report.singleAgent().p95LatencyMillis()).isPositive();
        assertThat(report.multiAgent().p95LatencyMillis()).isPositive();
        assertThat(report.recommendation()).isIn(
                "SINGLE_AGENT", "MULTI_AGENT", "REVIEW_TRADE_OFF");
    }

    private String summarize(AgentTrajectory trajectory, TrajectoryEvaluationResult result) {
        return "pass=" + result.passed()
                + ", tokens=" + trajectory.totalTokens()
                + ", cost=" + trajectory.totalCostUsd()
                + ", latencyMs=" + trajectory.durationMillis()
                + ", steps=" + trajectory.operationalStepCount();
    }
}
