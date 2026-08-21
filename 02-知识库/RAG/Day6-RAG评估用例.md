# Day 6：RAG 评估用例（召回 + 引用）

> Week 3 ｜ 归档：`05-记录/归档/2026-08-21-Week3-Day6-RAG评估.md` ｜ 代码：`com.enterprise.agent.rag.RagEvaluator`

## 一、为什么 RAG 也要评估

LLM 输出不是固定字符串，RAG 又多了一环检索，所以传统单元测试不够。需要专门衡量两件事：

1. **召回准不准**：检索有没有把「答案所在的文档」捞回来。
2. **引用对不对**：模型回答里的 [1][2] 是否真的指向正确的来源（防幻觉引用）。

## 二、两个指标（本日实现）

### recall@K（召回）

```java
public static boolean recallAtK(List<RetrievedChunk> chunks, String expectedDocumentId, int k) {
    return chunks.stream()
            .limit(k)                       // 只看前 K 条
            .anyMatch(chunk -> expectedDocumentId.equals(chunk.documentId()));
}
```

- 含义：期望文档是否出现在前 K 条检索结果里。
- K 常取 3；每提升一档 K，衡量的是「更宽松」的召回。

### citationAccuracy（引用准确率）

```java
private static final Pattern CITATION_PATTERN = Pattern.compile("\\[(\\d+)\\]");

public static boolean citationCorrect(String answer, List<RetrievedChunk> sources, String expectedDocumentId) {
    Matcher matcher = CITATION_PATTERN.matcher(answer);
    while (matcher.find()) {
        int index = Integer.parseInt(matcher.group(1)) - 1; // 引用是 1 起，转 0 起下标
        if (index >= 0 && index < sources.size()
                && expectedDocumentId.equals(sources.get(index).documentId())) {
            return true;
        }
    }
    return false;
}
```

- 含义：回答里引用的 `[n]` 是否指向正确来源。
- 正则 `\\[(\\d+)\\]` 提取 `[数字]`；`group(1)` 取括号里的数字。

## 三、评估套件（RagEvaluationService）

```java
public RagEvalMetrics evaluate(List<RagEvalCase> cases) {
    int recallHits = 0, citationHits = 0;
    for (RagEvalCase evalCase : cases) {
        RagChatResponse response = ragQaService.ask(evalCase.question(), null, 5);
        if (RagEvaluator.recallAtK(response.sources(), evalCase.expectedDocumentId(), 3)) recallHits++;
        if (RagEvaluator.citationCorrect(response.answer(), response.sources(), evalCase.expectedDocumentId())) citationHits++;
    }
    return RagEvaluator.metrics(cases.size(), recallHits, citationHits);
}
```

用例数据：`RagEvalCase(question, expectedDocumentId)`——准备多篇文档，每个问题标注「答案在哪份文档」。

## 四、实测结果（3 篇文档、3 个问题）

```
RagEvalMetrics[total=3, recallHits=3, citationHits=3,
               recallRate=1.0, citationAccuracy=1.0]
```

三个问题（年假/报销/电脑型号）全部检索到正确文档，且回答引用全部正确。

## 五、测试分层

- `RagEvaluatorTest`（5 个）：指标逻辑纯单测（recall@K、引用解析、metrics 计算），不依赖模型。
- `RagEvaluationServiceTest`（1 个）：mock 问答服务，验证评估循环。
- `RagEvaluationLiveTest`（1 个，真实 Embedding + DeepSeek）：端到端跑套件，输出指标。

## 六、Day 6 完成标准

- [x] 建立 RAG 评估用例
- [x] 能衡量召回（recall@K）与引用准确率