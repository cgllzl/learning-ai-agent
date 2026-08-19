# Sprint 3：企业知识库（RAG）

> 状态：2026-08-19 ｜ Day 2 完成：文档入库流程跑通（分块→Embedding→向量库）
## 目标
让 Agent 基于企业内部文档回答问题并给出引用。

## 任务
- [x] 文档入库流程：分块 → Embedding → 写入向量库
- [ ] 相似度检索 + 元数据过滤
- [ ] RAG 问答链路：检索 → 拼 Prompt → 生成 + 引用
- [ ] Reranking / Hybrid Search
- [ ] 基础 RAG 评估用例

## 技术
Embedding / Vector DB / Chunking / Rerank / Citation

## 知识库映射
- `02-知识库/RAG/`

## 完成标准
- [ ] 企业文档可检索
- [ ] 回答带引用来源
