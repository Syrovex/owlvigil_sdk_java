# 核心概念

## SDK 与 API 的关系

Java SDK 是 HTTP 客户端。Gateway 负责模型调用，Management 负责工作区和控制面资源；
两类 Key 与 API 地址不能混用。

## Client、响应与请求选项

创建一个可复用的线程安全 `OwlVigilClient`：

```java
try (var client = OwlVigilClient.builder().build()) {
    var response = client.gateway().listModels();
    System.out.println(response.value().data());
}
```

`ApiResponse<T>` 分为 `value()` 和 `meta()`。后者包含 HTTP 状态、响应头和 request ID。
`RequestOptions` 用于单次请求的 Workspace ID、超时、重试、幂等键和附加 Header。

## 请求生命周期

客户端默认超时 60 秒、最多重试 2 次。应用仍应为业务请求设置合理的单次超时；流式请求必须
通过关闭 `SseStream` 结束生命周期。

## Workspace、分页与幂等性

Management 资源通常属于 Workspace。Workspace ID 必须使用服务端返回的数字值。列表 cursor
是不透明字符串，只能原样传回。只有服务端明确支持的写操作才使用幂等键；网络超时不代表写
操作没有执行。

## Secret 与普通 ID

Gateway Key、Management API Key、Provider credential、OAuth Token 和 Webhook secret
属于 secret；Workspace ID、模型 ID 和 request ID 不是 secret，但仍应按业务敏感信息管理。

