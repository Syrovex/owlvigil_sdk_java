# Contributing to OwlVigil

Thank you for improving the OwlVigil SDK.

## Development

Use JDK 11 or later. The Gradle Wrapper downloads the pinned Gradle distribution.

```bash
./gradlew clean check
```

`check` runs the complete deterministic public-API suite and enforces JaCoCo
coverage of at least 95% of public lines and 85% of public branches. Internal
transport classes remain visible in the HTML/XML report but are excluded from
the public-surface gate. Open
`build/reports/jacoco/test/html/index.html` for the full report.

Run the opt-in read-only production test as a separate final gate:

```bash
OWLVIGIL_LIVE_TEST=1 \
OWLVIGIL_GATEWAY_KEY='replace-with-gateway-key' \
OWLVIGIL_API_KEY='replace-with-management-key' \
./gradlew clean check --rerun-tasks
```

The public suite must cover every exposed Gateway operation, Management
operation, OAuth2 flow, webhook verification path, request option, response
metadata type, and documented validation/error behavior. A new public method
must ship with a public-boundary contract test.

Keep changes focused, add contract tests for public behavior, and never add API
keys, access tokens, provider credentials, or webhook secrets to commits.

Open an issue before making a large API or compatibility change. Pull requests
should explain the affected API contract, tests run, compatibility impact, and
any credential requirements. By contributing, you agree that your contribution
is licensed under this repository's MIT License.
