# Sprint 3：企业知识库（RAG）

> 状态：2026-08-23 ｜ ✅ Week 3 全部完成（Day 1~7）｜ 周总结：`01-每周学习/Week-03-RAG/周总结.md`
## 目标
让 Agent 基于企业内部文档回答问题并给出引用。

## 任务
- [x] 文档入库流程：分块 → Embedding → 写入向量库
- [x] 相似度检索 + 元数据过滤
- [x] RAG 问答链路：检索 → 拼 Prompt → 生成 + 引用
- [x] Reranking / Hybrid Search
- [x] 基础 RAG 评估用例

## 技术
Embedding / Vector DB / Chunking / Rerank / Citation

## 知识库映射
- `02-知识库/RAG/`

## 完成标准
- [x] 企业文档可检索
- [x] 回答带引用来源
