# Management and Workspaces

## Management overview

`client.management()` handles control-plane operations such as Workspaces, usage, and request
logs. It does not perform model inference.

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

Source `0.3.0-SNAPSHOT` also contains typed Management methods; they are not part of published
`0.2.0`.

