# Management 与工作区

## Management 概览

`client.management()` 用于工作区、用量和请求日志等控制面操作，不负责模型推理。

### 列出工作区

```java
var page = client.management().listWorkspaces();
for (var workspace : page.value().items()) {
    System.out.println(workspace.id() + " " + workspace.name());
}
```

列表默认 50 条；使用 `listWorkspaces(cursor, limit)` 分页，limit 范围为 1～100，cursor
必须原样传回。使用工作区前应验证租户映射，不要默认使用第一项。

## 通用规则

Management 返回 `ApiResponse<T>`，保存 `meta().requestId()`。资源 ID 应来自服务端；
写操作超时后先读取状态，不要盲目重放。

## 用量与请求日志

```java
var usage = client.management().getUsageSummary(workspaceId);
var log = client.management().getRequestLog(workspaceId, requestId);
```

源码中的 `0.3.0-SNAPSHOT` 还包含类型化 Management 方法；它们不属于已发布的
`0.2.0`。

