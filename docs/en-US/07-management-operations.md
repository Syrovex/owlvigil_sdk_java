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

Source `0.3.0-SNAPSHOT` provides typed operations across the public Management contract through
`GeneratedManagementClient`. Published `0.2.0` contains only the convenience methods described
by the README and API reference. Do not copy source method names without checking your dependency version.
