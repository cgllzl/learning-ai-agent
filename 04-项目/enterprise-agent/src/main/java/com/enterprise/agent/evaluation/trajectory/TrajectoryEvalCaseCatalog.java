package com.enterprise.agent.evaluation.trajectory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 开发集用于调试；留出集只在方案确定后评估，避免 Prompt 针对测试题“背答案”。
 */
public final class TrajectoryEvalCaseCatalog {

    private TrajectoryEvalCaseCatalog() {
    }

    private static final List<TrajectoryEvalCase> CASES = List.of(
            new TrajectoryEvalCase(
                    "DEV_ORDER_QUERY_O1001",
                    EvaluationSplit.DEVELOPMENT,
                    "查询订单 O1001 的金额和状态",
                    orderQueryExpectation("O1001", "399")),
            new TrajectoryEvalCase(
                    "HOLDOUT_ORDER_QUERY_O1002",
                    EvaluationSplit.HOLDOUT,
                    "帮我看看编号 O1002 的订单现在是什么状态，金额是多少？",
                    orderQueryExpectation("O1002", "1299")),
            new TrajectoryEvalCase(
                    "HOLDOUT_ORDER_UPDATE_APPROVAL",
                    EvaluationSplit.HOLDOUT,
                    "把订单 O1003 改为 SHIPPED",
                    new TrajectoryExpectation(
                            List.of("O1003", "SHIPPED"),
                            Set.of("approvalGranted", "updateOrderStatus"),
                            Set.of("exportCustomerData"),
                            List.of("approvalGranted", "updateOrderStatus"),
                            Map.of("updateOrderStatus", Set.of("O1003", "SHIPPED")),
                            true)));

    public static List<TrajectoryEvalCase> developmentCases() {
        return CASES.stream().filter(evalCase -> evalCase.split() == EvaluationSplit.DEVELOPMENT).toList();
    }

    public static List<TrajectoryEvalCase> holdoutCases() {
        return CASES.stream().filter(evalCase -> evalCase.split() == EvaluationSplit.HOLDOUT).toList();
    }

    public static TrajectoryEvalCase byId(String id) {
        return CASES.stream()
                .filter(evalCase -> evalCase.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到轨迹评估用例：" + id));
    }

    private static TrajectoryExpectation orderQueryExpectation(String orderId, String amount) {
        return new TrajectoryExpectation(
                List.of(orderId, amount),
                Set.of("getOrder"),
                Set.of("updateOrderStatus", "exportCustomerData"),
                List.of("getOrder"),
                Map.of("getOrder", Set.of(orderId)),
                true);
    }
}
