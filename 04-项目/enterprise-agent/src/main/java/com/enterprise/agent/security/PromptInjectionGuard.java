package com.enterprise.agent.security;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 基于规则的提示词注入检测器（Day 4 基线版）。
 *
 * 注意：规则只能挡住最常见的直接注入，不能替代生产环境的多层防护。
 * 它主要用于演示「在调用大模型之前，先做一层输入检查」。
 */
public class PromptInjectionGuard {

    private static final List<Pattern> SUSPICIOUS_PATTERNS = List.of(
            Pattern.compile("(?i)(ignore|disregard|forget)\\s+(all|previous|above|the)\\s+(instructions|prompts?)"),
            Pattern.compile("(?i)(忽略|忘掉|忘记)\\s*(所有|以上|之前|先前)?\\s*(指令|提示|规则|系统提示词)"),
            Pattern.compile("(?i)(系统提示词|你的指令|你的提示词|你的系统提示|system\\s*prompt)"),
            Pattern.compile("(?i)(扮演|角色扮演|roleplay|jailbreak|\\bDAN\\b|do\\s+anything\\s+now)"),
            Pattern.compile("(?i)(开发者模式|没有限制|不受任何限制|无视所有规则)"),
            Pattern.compile("(?i)(base64|解码后执行|将下面的内容解码)"));

    public boolean isSuspicious(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return SUSPICIOUS_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(text).find());
    }
}
