# Management and Workspaces

## Management overview

In `0.3.0`, `client.management()` exposes all 144 typed Management operations through its
inherited `GeneratedManagementClient` methods, alongside the Workspace, usage, and request-log
convenience helpers. It does not perform model inference.

### List Workspaces

```java
var page = client.management().listWorkspaces();
for (var workspace : page.value().items()) {
    System.out.println(workspace.id() + " " + workspace.name());
}
```

The default page size is 50. Use `listWorkspaces(cursor, limit)` for pagination; limit is 1
through 100 and the cursor must be passed back unchanged. Verify tenant mappings instead of
assuming that the first item is production.

## General rules

Management returns `ApiResponse<T>`; retain `meta().requestId()`. Resource IDs must come from
the service. After a write timeout, read current state before replaying a request.

## Usage and request logs

```java
var usage = client.management().getUsageSummary(workspaceId);
var log = client.management().getRequestLog(workspaceId, requestId);
```

The generated methods and their `ManagementModels` request and response types are available
starting with `0.3.0`. Version `0.2.0` contains only the convenience helpers shown above.
See the [API reference](10-reference-examples.md) for the generated method index.

