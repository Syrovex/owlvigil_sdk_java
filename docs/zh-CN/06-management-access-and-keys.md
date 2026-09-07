# 成员、权限与 Gateway Key

## Management：成员与访问控制

成员权限由 Management scope、Workspace 角色和成员覆盖共同决定。先读取服务端角色与权限目录，
再进行变更，不要硬编码权限名称。

常见 Java 方法由生成客户端提供，例如：
`getWorkspacesWorkspaceIdMembers`、`postWorkspacesWorkspaceIdMembers`、
`getWorkspacesWorkspaceIdRoles` 和 `putWorkspacesWorkspaceIdMembersMemberIdPermissions`。
这些类型化方法属于 `0.3.0-SNAPSHOT`，使用时确认依赖版本。

## Gateway Key 管理

Gateway Key 是模型调用凭证，不是 Management API Key。创建、轮换、启用、禁用和删除前确认
Workspace、Key ID 与副作用。完整 Key 只在创建或轮换时返回，应立即保存到密钥管理服务。

Management API Key 使用 `client.management()`；Gateway Key 使用 `client.gateway()`。
不要把 Gateway Key 放入浏览器、日志或工单。
