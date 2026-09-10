package com.enterprise.agent.security.agentic;

import java.util.Set;

public record ContentInspection(boolean safe, Set<AgenticRisk> risks, String reason) {

    public ContentInspection {
        risks = Set.copyOf(risks);
    }
}
