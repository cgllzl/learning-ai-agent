package com.enterprise.agent.workflow;

/**
 * 质量复核结果。模型只允许给出 PASS 或 REVISE:原因。
 */
record ReviewDecision(boolean passed, String feedback) {

    static ReviewDecision fromModelOutput(String output) {
        if (output == null) {
            return new ReviewDecision(false, "复核模型没有返回结果");
        }

        String trimmed = output.trim();
        if ("PASS".equalsIgnoreCase(trimmed)) {
            return new ReviewDecision(true, "");
        }
        if (trimmed.regionMatches(true, 0, "REVISE:", 0, "REVISE:".length())) {
            String feedback = trimmed.substring("REVISE:".length()).trim();
            return new ReviewDecision(false, feedback.isBlank() ? "答复需要修改" : feedback);
        }
        if (trimmed.regionMatches(true, 0, "REVISE：", 0, "REVISE：".length())) {
            String feedback = trimmed.substring("REVISE：".length()).trim();
            return new ReviewDecision(false, feedback.isBlank() ? "答复需要修改" : feedback);
        }
        return new ReviewDecision(false, "复核输出不符合协议：" + trimmed);
    }
}
