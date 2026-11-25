# Changelog

This project follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and uses [Semantic Versioning](https://semver.org/). During the 0.x preview,
minor releases may include announced breaking changes.

## [Unreleased]

### Added

- 144 typed Management operations and generated request/response models from the standalone public Swagger contract, retaining the original three convenience methods.
- Canonical member-status and support-issue enums.
- Complete local contract coverage and a repeatable Python generator. Webhook live testing remains deferred.

### Fixed

- Deliver each complete SSE event without reading the following event first.
  On connections held open by the server, this prevents deltas, named errors
  and terminal frames from waiting for another event or being masked by a
  timeout. This fix is unreleased; Maven `0.2.0` remains affected.
- `ApiResponse` construction for HTTP 204 responses (`value()` is now `null` instead of throwing).

## [0.2.0] - 2026-08-28

### Added

- Initial Gateway, Management, streaming, error-handling, retry, and webhook
  functionality.
- Open-source governance, CI, packaging, and release configuration.

[Unreleased]: https://github.com/Syrovex/owlvigil_sdk_java/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/Syrovex/owlvigil_sdk_java/releases/tag/v0.2.0
