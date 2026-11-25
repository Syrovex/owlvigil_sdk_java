# Routing, Observability, and Billing

## Model routing and Providers

Applications submit a model name; Provider credentials remain server-side. Use the generated
Gateway Provider, Models, Routes, and Policies methods to inspect or change routing. Preview
production changes first, then verify actual inference with a non-production Key.

## Usage, logs, and audit

Use `getUsageSummary(workspaceId)` for usage and
`getRequestLog(workspaceId, requestId)` to investigate an individual request. A request ID is
for correlation and troubleshooting, not a secret. Confirm the environment and permissions before
high-impact financial, payment, or billing operations.

## Typed Management API

Version `0.3.0` provides all 144 typed operations across the reviewed public Management contract
through `GeneratedManagementClient`, inherited by `client.management()`. Version `0.2.0`
contains only the Workspace-listing, usage-summary, and request-log convenience helpers.
Use the `0.3.0` dependency when following these generated-method examples.
