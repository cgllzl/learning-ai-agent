package com.enterprise.agent.workbench;

/** 来源标题和片段来自后端租户目录，不接受浏览器传入的引用。 */
public record WorkbenchCitation(String sourceId, String title, String excerpt) {
}
