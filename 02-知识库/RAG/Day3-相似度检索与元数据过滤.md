# Day 3：相似度检索 + 元数据过滤

> Week 3 ｜ 归档：`05-记录/归档/2026-08-19-Week3-Day3-检索接口.md` ｜ 代码：`com.enterprise.agent.rag`

## 一、本日目标

把 Day 2 入库的内容**查出来**：用户问一句话 → 找到最相关的片段。

## 二、核心步骤

```java
// 1. 查询文本也要向量化（和入库用同一个 Embedding 模型）
Response<Embedding> queryResponse = embeddingModel.embed(query);

// 2. 可选：按 documentId 做元数据过滤
Filter filter = MetadataFilterBuilder.metadataKey("documentId").isEqualTo("DOC1");

// 3. 相似度检索，取 TopK
EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
        .queryEmbedding(queryEmbedding)
        .maxResults(5)          // 取最相似的 5 条
        .minScore(0.0)          // 相似度下限
        .filter(filter)         // 元数据过滤
        .build();
EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
```

## 三、三个关键点

1. **查询也要 Embedding**：检索时把用户问题向量化，再和库里所有向量比相似度——这就是"语义检索"。
2. **TopK**：`maxResults` 控制返回条数。太大会带进噪音，太小会漏答案（后面 Day 5 用重排优化）。
3. **minScore**：相似度下限，过滤明显不相关的内容。

## 四、元数据过滤（新语法）

- 入库时每段都带 `documentId` 等元数据（Day 2）。
- 检索时用 `MetadataFilterBuilder` 构造过滤条件：

```java
Filter filter = MetadataFilterBuilder.metadataKey("documentId").isEqualTo("DOC1");
```

- 适用场景：只搜某份文档 / 某个部门 / 某个日期范围；还能做权限隔离（不同人只能搜到授权的文档）。

## 五、接口

```http
POST /rag/search
{ "query": "公司年假有几天？", "documentId": "HR-001", "maxResults": 5 }
```

返回 `{ "chunks": [ { "text": "...", "score": 0.87, "documentId": "HR-001" } ] }`。

## 六、测试

- `DocumentRetrievalServiceTest`：最相似片段排最前、documentId 过滤生效（mock Embedding，离线）。
- `RagRetrieveControllerTest`：接口返回、空 query 400。

## 七、Day 3 完成标准

- [x] 相似度检索接口可用
- [x] 元数据过滤可用