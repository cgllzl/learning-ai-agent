package com.enterprise.agent.evaluation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentEvalCaseCatalogTest {

    @Test
    void catalogCoversCoreScenarios() {
        assertThat(AgentEvalCaseCatalog.all())
                .extracting(AgentEvalCase::id)
                .contains(
                        "ORDER_QUERY",
                        "ORDER_UPDATE_APPROVAL",
                        "RAG_CITATION",
                        "SUPERVISOR_ROUTING",
                        "MULTI_AGENT_MERGE",
                        "MCP_TOOL",
                        "RBAC_DENIED",
                        "TENANT_ISOLATION",
                        "PROMPT_INJECTION");
    }

    @Test
    void everyCaseHasEnterpriseContext() {
        assertThat(AgentEvalCaseCatalog.all())
                .allSatisfy(evalCase ->
                        assertThat(evalCase.enterpriseContext()).isNotBlank());
    }

    @Test
    void canLookupCaseById() {
        assertThat(AgentEvalCaseCatalog.byId("ORDER_QUERY").name()).isEqualTo("订单查询正确性");
    }
}
