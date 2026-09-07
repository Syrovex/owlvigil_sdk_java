# Management：路由、可观测性与账单

## 模型路由与 Provider

应用只提交模型名；Provider credential 保存在服务端。使用生成客户端中的 Gateway Provider、
Models、Routes 和 Policies 方法查看或修改路由。生产变更前先使用 preview，再用非生产 Key
验证实际调用。

## 用量、日志与审计

使用 `getUsageSummary(workspaceId)` 查看用量，使用
`getRequestLog(workspaceId, requestId)` 排查单次请求。request ID 只能用于关联和排障，不要
当作 secret。高影响的财务、支付和账单操作必须在确认环境和权限后执行。

## 类型化 Management API

`0.3.0-SNAPSHOT` 的 `GeneratedManagementClient` 覆盖公开 Management 合约中的类型化
操作；已发布 `0.2.0` 只包含 README 和 API reference 所列的便捷方法。版本不匹配时不要
照抄源码中的方法名。
