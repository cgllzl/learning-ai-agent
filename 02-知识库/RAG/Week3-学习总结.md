# Week 3 学习总结：RAG —— 让模型会查企业知识

> 日期：2026-08-18 ~ 2026-08-23 ｜ 周总结：`01-每周学习/Week-03-RAG/周总结.md`

## 一、本周主线

解决「模型不知道公司内部知识」的问题：把文档分块、向量化入库，检索相关内容拼进 Prompt，让模型基于真实资料回答并给出引用。交付了完整的 RAG 系统：入库 → 检索 → 问答 → 评估。

![Week 3 学习路线图](images/Week3-学习路线图.png)

```mermaid
flowchart LR
    D1["Day 1 原理与选型"] --> D2["Day 2 文档入库"]
    D2 --> D3["Day 3 检索接口"]
    D3 --> D4["Day 4 RAG 问答"]
    D4 --> D5["Day 5 Hybrid Rerank"]
    D5 --> D6["Day 6 评估用例"]
    D6 --> D7["Day 7 周总结"]
```

## 二、七天内容回顾

| 天 | 主题 | 交付物 | 关键点 |
| --- | --- | --- | --- |
| Day 1 | Embedding 与向量检索原理 | 概念笔记 + 选型 | Embedding/相似度/ANN；选 InMemory + all-minilm |
| Day 2 | 文档入库 | `POST /rag/ingest` | 分块（300/30）→ Embedding → 向量库 |
| Day 3 | 检索接口 | `POST /rag/search` | 相似度 TopK + documentId 元数据过滤 |
| Day 4 | RAG 问答链路 | `POST /rag/chat` | 检索 → 拼 Prompt → 生成 + [n] 引用 |
| Day 5 | Hybrid + Rerank | `POST /rag/hybrid-search` | 向量 + 关键词 bigram + RRF 融合 |
| Day 6 | 评估用例 | `POST /rag/evaluate` | recall@K、严格引用、引用精确率 |
| Day 7 | 周总结 | 本文件 | 知识归纳 |

## 三、核心知识点

1. **RAG 三步走**：入库（Chunk → Embed → Store）、检索（Query → Embed → TopK）、生成（Context → Prompt → Answer + Citation）。
2. **Embedding**：文本 → 向量，语义相近距离近；余弦相似度最常用。
3. **分块（Chunking）**：块太大则检索不精准、超模型输入；块太小则丢失上下文。重叠（overlap）保证边界语义连续。
4. **元数据过滤**：入库带 `documentId`，检索用 `MetadataFilterBuilder` 按来源过滤，还能做权限隔离。
5. **Hybrid Search**：向量检索（语义）+ 关键词检索（字面）互补；字符 bigram 是免分词的中英文统一方案。
6. **Reranking（RRF）**：两路结果用倒数排名融合 `1/(k+rank)` 重新排序；生产可换 Cross-Encoder/LLM 重排。
7. **防幻觉引用**：Prompt 约束「只按资料答 + [n] 标注 + 没有就直说」；评估侧严格校验引用。
8. **RAG 评估**：recall@K（召回）、citationAccuracy（严格引用）、citationPrecision（精确率）。

## 四、常用语法速查

```java
// 分块
DocumentSplitter splitter = DocumentSplitters.recursive(300, 30);
List<TextSegment> segments = splitter.split(Document.from(content, metadata));

// 向量化 + 入库
List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
List<String> ids = embeddingStore.addAll(embeddings, segments);

// 检索（含过滤）
EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
        .queryEmbedding(embeddingModel.embed(query).content())
        .maxResults(5).minScore(0.0)
        .filter(MetadataFilterBuilder.metadataKey("documentId").isEqualTo("HR-001"))
        .build();
EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

// 关键词 bigram 打分
Set<String> grams = bigrams(text);   // 相邻两字符集合
double score = overlap(queryGrams, grams);  // 交集大小

// RRF 融合
score += 1.0 / (60 + rank);
```

## 五、验证与测试情况

- `mvn test` 全量通过：RAG 模块有入库/检索/问答/混合检索/文件上传/评估共 20+ 个用例。
- 真实联调（本地 Embedding + DeepSeek）：
  - 文档入库实测分块成功；RAG 问答实测带 [1] 引用。
  - 评估实测 3 篇文档 3 个问题：recallRate=1.0、citationAccuracy=1.0、citationPrecision=1.0。

## 六、面试问题对照

| 面试问题 | 本周答案 |
| --- | --- |
| RAG 为什么会召回错误内容？ | 分块不当/Embedding 弱/没过滤/TopK 太大带噪音 |
| Chunking 分太大/太小？ | 太大不精准且超输入，太小丢上下文（用 overlap 缓解） |
| 如何防幻觉引用？ | Prompt 约束 + 返回 sources + 评估侧严格校验引用 |
| 向量检索 vs 关键词检索？ | 向量看语义、关键词看字面，Hybrid + RRF 取长补短 |

## 七、下周预告（Week 4：Agent Orchestration + MCP）

把 Week 2 的 Tool Calling 和 Week 3 的 RAG 组合起来，学习多 Agent 编排（Supervisor/Handoff）与 MCP 标准化工具接入。