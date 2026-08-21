# Day 6：RAG 评估用例（召回 + 引用）

> Week 3 ｜ 归档：`05-记录/归档/2026-08-21-Week3-Day6-RAG评估.md` ｜ 代码：`com.enterprise.agent.rag.RagEvaluator`

## 一、为什么 RAG 也要评估

LLM 输出不是固定字符串，RAG 又多了一环检索，所以传统单元测试不够。需要专门衡量两件事：

1. **召回准不准**：检索有没有把「答案所在的文档」捞回来。
2. **引用对不对**：模型回答里的 [1][2] 是否真的指向正确的来源（防幻觉引用）。

## 二、三个指标（Day 6 补充后）

### 1. recall@K（召回）

```java
public static boolean recallAtK(List<RetrievedChunk> chunks, String expectedDocumentId, int k) {
    return chunks.stream()
            .limit(k)                       // 只看前 K 条
            .anyMatch(chunk -> expectedDocumentId.equals(chunk.documentId()));
}
```

- 含义：期望文档是否出现在前 K 条检索结果里（K 常取 3）。

### 2. citationAccuracy（严格引用正确率）

```java
public static boolean citationCorrect(String answer, List<RetrievedChunk> sources, String expectedDocumentId) {
    List<Integer> indices = citationIndices(answer);   // 提取所有 [n] 的 0 起下标
    if (indices.isEmpty()) {
        return false;                                   // 没引用直接判错
    }
    return indices.stream().allMatch(index -> isCorrect(index, sources, expectedDocumentId));
}
```

- **严格模式**：每一处引用都必须下标合法、且指向期望文档；只要有一处错，这道题就算引用错误。
- 这是对上一版「宽松模式」的修正——之前只要有一处对就返回 true，会漏掉「部分引用错误/幻觉引用」的问题。

### 3. citationPrecision（引用精确率）

```java
public static double citationPrecision(String answer, List<RetrievedChunk> sources, String expectedDocumentId) {
    List<Integer> indices = citationIndices(answer);
    if (indices.isEmpty()) return 0.0;
    long correct = indices.stream().filter(i -> isCorrect(i, sources, expectedDocumentId)).count();
    return (double) correct / indices.size();           // 正确引用数 / 总引用数
}
```

- 0~1 的连续指标：比布尔值更细，能量化「3 个引用对 2 个」这种情况。
- 配套：`citationCount(answer)` 统计总引用数、`correctCitationCount(...)` 统计正确数，评估套件里跨用例汇总。

## 三、评估套件（RagEvaluationService）

```java
public RagEvalMetrics evaluate(List<RagEvalCase> cases) {
    int recallHits = 0, citationHits = 0, totalCitations = 0, totalCorrect = 0;
    for (RagEvalCase evalCase : cases) {
        RagChatResponse response = ragQaService.ask(evalCase.question(), null, 5);
        if (RagEvaluator.recallAtK(response.sources(), evalCase.expectedDocumentId(), 3)) recallHits++;
        if (RagEvaluator.citationCorrect(response.answer(), response.sources(), evalCase.expectedDocumentId())) citationHits++;
        totalCitations += RagEvaluator.citationCount(response.answer());
        totalCorrect += RagEvaluator.correctCitationCount(response.answer(), response.sources(), evalCase.expectedDocumentId());
    }
    double precision = totalCitations == 0 ? 0.0 : (double) totalCorrect / totalCitations;
    return RagEvaluator.metrics(cases.size(), recallHits, citationHits, precision);
}
```

用例数据：`RagEvalCase(question, expectedDocumentId)`——准备多篇文档，每个问题标注「答案在哪份文档」。

## 四、实测结果（3 篇文档、3 个问题）

```
RagEvalMetrics[total=3, recallHits=3, citationHits=3,
               recallRate=1.0, citationAccuracy=1.0, citationPrecision=1.0]
```

三个问题（年假/报销/电脑型号）全部检索正确、引用全部正确、精确率 1.0。

## 五、测试分层

- `RagEvaluatorTest`（8 个）：recall@K、严格引用（全对/有一错/越界/无引用）、引用精确率、metrics 计算。
- `RagEvaluationServiceTest`（1 个）：mock 问答服务，验证评估循环与汇总。
- `RagEvaluationLiveTest`（1 个，真实 Embedding + DeepSeek）：端到端跑套件，输出指标。

## 六、Day 6 完成标准

- [x] 建立 RAG 评估用例
- [x] 能衡量召回（recall@K）、严格引用正确率、引用精确率


## 七、HTTP 评估工作流（便于用自己上传的文件测）

1. 上传文档：`POST /rag/ingest/file`（multipart，txt/md，可传多篇，documentId 不填则用文件名）。
2. 跑评估：`POST /rag/evaluate`，Body 传 cases（每项 question + expectedDocumentId）。
3. 返回 `RagEvalMetrics`（total / recallRate / citationAccuracy / citationPrecision）。
