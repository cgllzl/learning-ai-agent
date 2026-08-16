# Week 2 Day 1 归档：Function Calling 原理（2026-08-16）

> 学习计划：`01-每周学习/Week-02-Tool-Calling/学习目标.md` ｜ 笔记：`02-知识库/Tool-Calling/Day1-Function-Calling原理.md`

## 今天学了什么

1. **Function Calling 是什么**：模型输出「调用哪个工具 + 什么参数」的结构化意图，真正执行在 Java 侧；模型不执行代码。
2. **Tool 定义三要素**：name（工具名）、description（描述，决定模型选哪个）、parameters（参数 JSON Schema）。
3. **Agent Loop**：提问 → 模型决策 → 执行工具 → 结果回填 → 继续，直到模型认为足够（防死循环需最大轮数限制，Day 6 学）。
4. **何时用 Tool**：需要实时/业务数据或副作用时；纯问答考虑 RAG（Week 3）。

## 完成标准（Day 1）
- [x] 理解 Function Calling 原理
- [x] 通读 LangChain4j Tools 官方文档要点

## 下一步（Day 2）
- 写第一个 Java Tool（@Tool/@P）并注册给 LLM