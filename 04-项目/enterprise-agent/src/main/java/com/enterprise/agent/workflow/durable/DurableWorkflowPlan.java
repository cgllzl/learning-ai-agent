package com.enterprise.agent.workflow.durable;

import java.util.List;
import java.util.Locale;

record DurableWorkflowPlan(List<AgentStep> steps, boolean requiresHuman, String reason) {

    private static final String APPROVED_PLAN = "UPDATE_ORDER,SEND_NOTIFICATION";

    DurableWorkflowPlan {
        steps = List.copyOf(steps);
    }

    static DurableWorkflowPlan fromModelOutput(String output) {
        if (output == null) {
            return human("计划模型没有返回结果");
        }
        String normalized = output.trim()
                .replace("```text", "")
                .replace("```", "")
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
        if (APPROVED_PLAN.equals(normalized)) {
            return new DurableWorkflowPlan(
                    List.of(AgentStep.UPDATE_ORDER, AgentStep.SEND_NOTIFICATION),
                    false,
                    "计划通过白名单校验"
            );
        }
        if ("HUMAN".equals(normalized)) {
            return human("计划模型要求转人工");
        }
        return human("计划输出不符合白名单：" + output.trim());
    }

    private static DurableWorkflowPlan human(String reason) {
        return new DurableWorkflowPlan(List.of(), true, reason);
    }
}
