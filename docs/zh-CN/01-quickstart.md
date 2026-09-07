# 快速开始：完成第一次模型调用

本章完成 Java SDK 的安装、认证和第一次 Gateway 请求。

## 准备工作

- Java 11 或更高版本。
- 一个 OwlVigil Gateway Key。
- 一个 Maven 或 Gradle 项目。

不要把真实凭证写入源码、Git、日志或前端代码。

## 安装 SDK

Maven：

```xml
<dependency>
  <groupId>com.owlvigil</groupId>
  <artifactId>owlvigil-java</artifactId>
  <version>0.2.0</version>
</dependency>
```

Gradle Kotlin DSL：

```kotlin
implementation("com.owlvigil:owlvigil-java:0.2.0")
```

设置凭证：

```bash
export OWLVIGIL_GATEWAY_KEY='your-gateway-key'
```

## 创建并运行程序

```java
import com.owlvigil.OwlVigilClient;
import com.owlvigil.gateway.ChatCompletionRequest;
import com.owlvigil.gateway.Message;

public class Main {
    public static void main(String[] args) {
        try (var client = OwlVigilClient.builder().build()) {
            var result = client.gateway().createChatCompletion(
                    ChatCompletionRequest.builder()
                            .model("your-model-id")
                            .message(Message.of("user", "用一句话介绍 OwlVigil。"))
                            .maxTokens(64)
                            .build());

            if (result.value().choices().isEmpty()) {
                throw new IllegalStateException("empty response");
            }
            System.out.println(result.value().choices().get(0).message().content());
            System.out.println("request_id=" + result.meta().requestId());
        }
    }
}
```

先调用 `client.gateway().listModels()` 确认当前 Key 可用的模型，再替换
`your-model-id`。模型可用性由 Workspace、Provider、路由和 Key 共同决定。

## 使用 Management

Management 使用另一类 Key：

```bash
export OWLVIGIL_API_KEY='your-management-api-key'
```

```java
try (var client = OwlVigilClient.builder().build()) {
    var workspaces = client.management().listWorkspaces();
    for (var workspace : workspaces.value().items()) {
        System.out.println(workspace.id() + " " + workspace.name());
    }
}
```

常见失败：401 检查 Key 类型和状态；403 检查 scope 与 Workspace 权限；429 遵循服务端等待提示。

