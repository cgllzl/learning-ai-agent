package com.enterprise.agent.security.agentic;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 对用户、RAG、Memory、Tool 输出和 Agent 消息使用同一套“不可信内容”基线检查。
 * 规则检测只是第一道闸门，生产环境还应叠加模型分类器、内容净化和人工复核。
 */
@Component
public class AgenticContentGuard {

    private static final List<RiskPattern> PATTERNS = List.of(
            risk(AgenticRisk.INSTRUCTION_OVERRIDE,
                    "(?i)(ignore|disregard|forget).{0,20}(instructions?|prompts?)",
                    "(?i)(忽略|无视|忘掉|忘记).{0,12}(指令|提示|规则)"),
            risk(AgenticRisk.SYSTEM_PROMPT_DISCLOSURE,
                    "(?i)(reveal|print|show).{0,20}system\\s*prompt",
                    "(?i)(打印|显示|泄露|输出).{0,12}(系统提示词|内部指令)"),
            risk(AgenticRisk.TOOL_COMMAND_IN_DATA,
                    "(?i)(call|invoke|execute|use).{0,20}(tool|function|exportCustomerData)",
                    "(?i)(调用|执行|使用).{0,12}(工具|函数|exportCustomerData)"),
            risk(AgenticRisk.DATA_EXFILTRATION,
                    "(?i)(send|upload|export|exfiltrate).{0,40}(customer|secret|token|credential).{0,40}(https?://|email|attacker|evil)",
                    "(?i)(发送|上传|导出|外传).{0,40}(客户|密钥|令牌|凭证).{0,40}(http|邮箱|外部|攻击者|evil)"),
            risk(AgenticRisk.PRIVILEGE_ESCALATION,
                    "(?i)(treat|grant|set).{0,30}(admin|administrator|root)",
                    "(?i)(设为|视为|授予).{0,20}(管理员|超级用户|root)"),
            risk(AgenticRisk.MEMORY_POISONING,
                    "(?i)(remember|store).{0,20}(forever|permanently|from now on).{0,40}(admin|bypass|ignore)",
                    "(?i)(永久记住|写入长期记忆|以后都记住).{0,40}(管理员|绕过|忽略|无视)"));

    public ContentInspection inspect(UntrustedContent content) {
        if (content == null || content.text() == null || content.text().isBlank()) {
            return new ContentInspection(true, EnumSet.noneOf(AgenticRisk.class), "内容为空");
        }

        EnumSet<AgenticRisk> risks = EnumSet.noneOf(AgenticRisk.class);
        for (RiskPattern entry : PATTERNS) {
            if (entry.pattern().matcher(content.text()).find()) {
                risks.add(entry.risk());
            }
        }
        return risks.isEmpty()
                ? new ContentInspection(true, risks, "未发现基线风险")
                : new ContentInspection(false, risks,
                "不可信来源 " + content.source() + " 命中风险 " + risks);
    }

    public void requireSafe(UntrustedContent content) {
        ContentInspection inspection = inspect(content);
        if (!inspection.safe()) {
            throw new AgenticSecurityBlockedException(inspection.reason(), inspection.risks());
        }
    }

    private static RiskPattern risk(AgenticRisk risk, String english, String chinese) {
        return new RiskPattern(risk, Pattern.compile(english + "|" + chinese));
    }

    private record RiskPattern(AgenticRisk risk, Pattern pattern) {
    }
}
