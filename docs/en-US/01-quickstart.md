# Quickstart: Make Your First Model Call

This chapter targets Java SDK `0.3.0`, configures authentication, and makes a
first Gateway request.

**Publication pending:** `0.3.0` is currently a release candidate in this source
branch. Wait for Maven Central publication before using the dependency
declarations below. See [Versioning](README.md#versioning) for the differences
from `0.2.0`.

## Prerequisites

- Java 11 or later.
- An OwlVigil Gateway Key.
- A Maven or Gradle project.

Never put real credentials in source control, logs, or client-side code.

## Install the SDK

Maven:

```xml
<dependency>
  <groupId>com.owlvigil</groupId>
  <artifactId>owlvigil-java</artifactId>
  <version>0.3.0</version>
</dependency>
```

Gradle Kotlin DSL:

```kotlin
implementation("com.owlvigil:owlvigil-java:0.3.0")
```

Set the credential:

```bash
export OWLVIGIL_GATEWAY_KEY='your-gateway-key'
```

## Create and run a program

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
                            .message(Message.of("user", "Describe OwlVigil in one sentence."))
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

Call `client.gateway().listModels()` first and replace `your-model-id`
with a model available to the current Key. Availability depends on the
Workspace, Providers, routes, and Key.

## Use Management

Management uses a separate credential:

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

Common failures: check credential type and status for 401, scope and Workspace
permissions for 403, and follow the server wait guidance for 429.

