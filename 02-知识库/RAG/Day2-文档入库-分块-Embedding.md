# Day 2：文档入库（分块 → Embedding → 写入向量库）

> Week 3 ｜ 归档：`05-记录/归档/2026-08-19-Week3-Day2-文档入库.md` ｜ 代码：`com.enterprise.agent.rag`

## 一、本日目标

跑通 RAG 的第一半：把企业文档变成向量库里可检索的片段。

```mermaid
flowchart LR
    A[原始文档] --> B[分块 Chunking]
    B --> C[Embedding 向量化]
    C --> D[写入向量库]
```

## 二、分块（Chunking）

```java
private final DocumentSplitter splitter = DocumentSplitters.recursive(300, 30);
List<TextSegment> segments = splitter.split(Document.from(content, md));
```

- `DocumentSplitters.recursive(maxSegmentSize, maxOverlapSize)`：递归按字符切块。
- `300`：每块最多 300 字符；`30`：相邻块重叠 30 字符，保证语义在边界处不断裂。
- **为什么要分块**：Embedding 模型有输入长度上限，且整篇文档塞进 Prompt 又长又贵；切成合适小块，检索更精准。
- **为什么重叠**：避免一句话刚好被切断，丢失上下文。

## 三、Embedding（向量化）

```java
@Bean
EmbeddingModel embeddingModel() {
    return new AllMiniLmL6V2EmbeddingModel();   // 本地 ONNX 模型，无需 API Key
}

Response<List<Embedding>> response = embeddingModel.embedAll(segments);
List<Embedding> embeddings = response.content();   // 每个片段 → 一个 384 维向量
```

- `AllMiniLmL6V2EmbeddingModel`：本地运行（ONNX），首次使用会下载约 90MB 模型文件；384 维。
- `embedAll(List<TextSegment>)`：批量向量化，比逐个 embed 更快。

## 四、写入向量库

```java
@Bean
EmbeddingStore<TextSegment> embeddingStore() {
    return new InMemoryEmbeddingStore<>();   // 学习期内存版
}

List<String> segmentIds = embeddingStore.addAll(embeddings, segments);
```

- `InMemoryEmbeddingStore<TextSegment>`：LangChain4j 内置，重启即丢失，学习期够用；生产换 Qdrant/pgvector 接口不变。
- `addAll(embeddings, segments)` 返回每个片段自动生成的 id（供引用/删除用）。

## 五、关键代码全景

```java
public IngestionResult ingest(String documentId, String content, Map<String, Object> metadata) {
    Metadata md = new Metadata();
    md.put("documentId", documentId);                  // 元数据：来源追踪、后续过滤
    if (metadata != null) {
        metadata.forEach((k, v) -> md.put(k, v == null ? "" : v.toString()));
    }

    List<TextSegment> segments = splitter.split(Document.from(content, md));  // 1 分块
    List<Embedding> embeddings = embeddingModel.embedAll(segments).content(); // 2 向量化
    List<String> segmentIds = embeddingStore.addAll(embeddings, segments);    // 3 入库
    return new IngestionResult(documentId, segments.size(), segmentIds);
}
```

## 六、踩坑（新学到的细节）

- **`Metadata.put` 只有类型化重载**（`put(String, String/int/double/float)`），没有 `put(String, Object)`。所以 `Map<String,Object>` 遍历时直接 `md::put` 会编译失败，需要转成 `value.toString()` 或按类型分派。
- **本地模型下载**：`AllMiniLmL6V2EmbeddingModel` 首次运行下载 ONNX 模型；单测里 mock 掉 `EmbeddingModel` 避免每次下载，真实联调用 `RUN_ONNX_TESTS=1` 显式开启。

## 七、测试

- `DocumentIngestionServiceTest`（3 个）：长文本分多块、元数据带 documentId、片段写入向量库（mock Embedding，快速离线）。
- `RagIngestControllerTest`（2 个）：POST /rag/ingest 返回结果、空 content 400。
- `RagIngestionLiveTest`（1 个，真实模型）：RUN_ONNX_TESTS=1 时真实入库，实测长文本切成 2 块。

## 八、Day 2 完成标准

- [x] 文档能分块 → Embedding → 写入向量库
- [x] 真实本地 Embedding 模型跑通（实测分块数 2）

## 九、如何本地测试

### 1. 单测（最快，不下载模型）
```powershell
cd F:\ChatGPT\学习之路\04-项目\enterprise-agent
mvn test -Dtest=DocumentIngestionServiceTest,RagIngestControllerTest
```
用 mock 的 Embedding，验证分块、元数据、入库逻辑与接口校验，秒级跑完。

### 2. 真实本地模型入库（首次下载约 90MB）
```powershell
.\scripts\test-rag-live.ps1
```
脚本会自动：强制控制台 UTF-8（解决中文乱码）、设置 `RUN_ONNX_TESTS=1`、运行 `RagIngestionLiveTest`。输出里能看到 `[入库结果] 分块数 = N`。

### 3. 手动 HTTP 体验
```powershell
.\scripts\run-dev.ps1   # 先启动服务
```
然后 Apifox / IDEA 的 requests.http 发：
```http
POST http://localhost:8080/rag/ingest
Content-Type: application/json

{ "documentId": "DOC1", "content": "Java 21 引入了虚拟线程……" }
```
返回 `{"documentId":"DOC1","segmentCount":1,"segmentIds":["..."]}`。

### 注意
- 当前是内存向量库，重启服务后数据丢失，属预期行为。
- 检索查询接口是 Day 3 内容，Day 2 只能验证「入库成功」。
- 中文乱码的根因与解决：Windows PowerShell 5.1 控制台默认 GBK 输出，而 Maven/Java 输出 UTF-8；脚本内 `[Console]::OutputEncoding = UTF8` + `chcp 65001` 即可（已内置到 test-rag-live.ps1）。


## 十、附：文件上传入库接口（multipart）

- `POST /rag/ingest/file`：multipart 上传，`file` 字段选本地 txt/md，可选 `documentId`（不填用文件名），单文件上限 10MB。
- 实现：`RagIngestFileController`——校验扩展名（txt/md/markdown）→ 读 UTF-8 文本 → 复用 `DocumentIngestionService` 入库，并在元数据里记 `fileName`。
- 测试：`RagIngestFileControllerTest` 4 个用例（txt 派生 id / md 指定 id / 拒绝非文本类型 / 拒绝空文件）。
