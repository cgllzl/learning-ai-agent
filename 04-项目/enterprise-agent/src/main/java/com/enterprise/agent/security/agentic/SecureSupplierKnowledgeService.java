package com.enterprise.agent.security.agentic;

import com.enterprise.agent.security.AuditLogService;
import com.enterprise.agent.security.AuditStatus;
import com.enterprise.agent.security.SecuritySubject;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 企业供应商知识问答的安全入口：所有内容入模前扫描，助手不配置任何 Tool。
 */
@Service
public class SecureSupplierKnowledgeService {

    private final SecureSupplierKnowledgeAssistant assistant;
    private final AgenticContentGuard contentGuard;
    private final AuditLogService auditLog;

    @Autowired
    public SecureSupplierKnowledgeService(@Qualifier("openAiChatModel") OpenAiChatModel chatModel,
                                          AgenticContentGuard contentGuard,
                                          AuditLogService auditLog) {
        this(
                AiServices.builder(SecureSupplierKnowledgeAssistant.class)
                        .chatModel(chatModel)
                        .build(),
                contentGuard,
                auditLog
        );
    }

    SecureSupplierKnowledgeService(SecureSupplierKnowledgeAssistant assistant,
                                   AgenticContentGuard contentGuard,
                                   AuditLogService auditLog) {
        this.assistant = assistant;
        this.contentGuard = contentGuard;
        this.auditLog = auditLog;
    }

    public String answer(SecuritySubject subject,
                         String question,
                         List<UntrustedContent> contextItems) {
        requireSafeAndAudit(subject, new UntrustedContent(
                UntrustedContentSource.USER_INPUT, "user-question", question));
        contextItems.forEach(item -> requireSafeAndAudit(subject, item));

        String context = contextItems.stream()
                .map(item -> "[来源=" + item.source() + ", id=" + item.sourceId() + "]\n" + item.text())
                .collect(Collectors.joining("\n\n"));
        String answer = assistant.answer(question, context);
        auditLog.record(subject, "secureSupplierKnowledge", "sourceCount=" + contextItems.size(),
                "ANSWERED", AuditStatus.SUCCESS);
        return answer;
    }

    private void requireSafeAndAudit(SecuritySubject subject, UntrustedContent content) {
        ContentInspection inspection = contentGuard.inspect(content);
        if (!inspection.safe()) {
            auditLog.record(subject, "contentGuard:" + content.source(),
                    "sourceId=" + content.sourceId(), inspection.reason(), AuditStatus.BLOCKED);
            throw new AgenticSecurityBlockedException(inspection.reason(), inspection.risks());
        }
    }
}
