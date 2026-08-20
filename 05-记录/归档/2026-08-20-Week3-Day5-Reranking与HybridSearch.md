# Week 3 Day 5 归档：Reranking 与 Hybrid Search（2026-08-20）

> 学习计划：`01-每周学习/Week-03-RAG/学习目标.md` ｜ 笔记：`02-知识库/RAG/Day5-Reranking与HybridSearch.md`

## 今天做了什么

1. `InMemoryCorpus`：关键词索引（入库同步写入原文片段）。
2. `HybridSearchService`：向量检索 + 关键词检索（字符 bigram 打分）+ RRF 融合重排。
3. `RagQaService` 改用混合检索；新增 `POST /rag/hybrid-search`。
4. 测试：HybridSearchServiceTest 2 个 + 控制器 2 个；端到端 RAG 问答实测通过（回答带 [1] 引用）。

## 新学到的技术点

- 向量检索 vs 关键词检索的互补关系。
- 字符 bigram（n-gram）做中英文统一的关键词打分，无需分词器。
- RRF（Reciprocal Rank Fusion）倒数排名融合：1/(k+rank)，k=60。
- Reranking 的含义：对初检候选重新打分排序；RRF 是零成本版，生产可用 Cross-Encoder/LLM 重排。

## 完成标准（Day 5）

- [x] 加入 Reranking 与 Hybrid Search

## 下一步（Day 6）

- RAG 评估用例（召回是否准确、引用是否正确）