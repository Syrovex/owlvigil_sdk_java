# Members, Access Control, and Gateway Keys

## Management: members and access control

Member authority combines Management scopes, Workspace roles, and member overrides. Read the
server-provided role and permission catalogs before making changes; do not hard-code permission names.

The generated client provides typed methods such as
`getWorkspacesWorkspaceIdMembers`, `postWorkspacesWorkspaceIdMembers`,
`getWorkspacesWorkspaceIdRoles`, and
`putWorkspacesWorkspaceIdMembersMemberIdPermissions`. These methods are part of
`0.3.0-SNAPSHOT`; confirm the dependency version before using them.

## Gateway Key management

A Gateway Key authenticates model calls; it is not a Management API Key. Confirm the Workspace,
Key ID, and side effects before creating, rotating, enabling, disabling, or deleting a key. The
complete key is returned only on create or rotate and must be stored immediately in a secret manager.

Use `client.management()` with a Management API Key and `client.gateway()` with a Gateway Key.
Never put a Gateway Key in browser code, logs, or tickets.
