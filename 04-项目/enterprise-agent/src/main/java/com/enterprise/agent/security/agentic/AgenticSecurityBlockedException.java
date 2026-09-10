package com.enterprise.agent.security.agentic;

import java.util.Set;

public class AgenticSecurityBlockedException extends RuntimeException {

    private final Set<AgenticRisk> risks;

    public AgenticSecurityBlockedException(String message, Set<AgenticRisk> risks) {
        super(message);
        this.risks = Set.copyOf(risks);
    }

    public AgenticSecurityBlockedException(String message, AgenticRisk risk) {
        this(message, Set.of(risk));
    }

    public Set<AgenticRisk> risks() {
        return risks;
    }
}
