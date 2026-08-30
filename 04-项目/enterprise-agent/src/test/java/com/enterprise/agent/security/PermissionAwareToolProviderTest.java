package com.enterprise.agent.security;

import com.enterprise.agent.agent.MockOrderData;
import com.enterprise.agent.agent.OrderTools;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionAwareToolProviderTest {

    private final PermissionAwareToolProvider provider =
            new PermissionAwareToolProvider(new OrderTools(new MockOrderData()));

    @Test
    void customerServiceSeesReadToolsButNotUpdateStatus() {
        List<String> toolNames = provideFor(new SecuritySubject("u1", "t1", Set.of("CUSTOMER_SERVICE")));

        assertThat(toolNames)
                .contains("getOrder", "getUser", "getProduct", "getLogistics")
                .doesNotContain("updateOrderStatus");
    }

    @Test
    void orderAdminSeesOnlyUpdateStatusTool() {
        List<String> toolNames = provideFor(new SecuritySubject("u2", "t1", Set.of("ORDER_ADMIN")));

        assertThat(toolNames).containsExactly("updateOrderStatus");
    }

    @Test
    void unknownRoleGetsNoTools() {
        List<String> toolNames = provideFor(new SecuritySubject("u3", "t1", Set.of("EMPLOYEE")));

        assertThat(toolNames).isEmpty();
    }

    private List<String> provideFor(SecuritySubject subject) {
        return TenantContext.run(subject, () -> {
            ToolProviderRequest request = new ToolProviderRequest("test-memory-id", UserMessage.from("查询订单"));
            ToolProviderResult result = provider.provideTools(request);
            return result.aiServiceTools().stream()
                    .map(AiServiceTool::name)
                    .toList();
        });
    }
}
