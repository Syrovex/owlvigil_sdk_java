# Releasing

1. Verify `com.owlvigil` in Sonatype Central Portal and configure the
   `maven-central` GitHub environment with `MAVEN_CENTRAL_USERNAME`,
   `MAVEN_CENTRAL_PASSWORD`, `GPG_PRIVATE_KEY`, and
   `GPG_PRIVATE_KEY_PASSWORD`.
2. Update `version` in `build.gradle.kts`, public User-Agent version, README,
   and `CHANGELOG.md` in one pull request.
3. Run `./gradlew clean test jar sourcesJar javadocJar` and review the POM.
4. Merge the reviewed release commit and create the matching signed tag, for
   example `v0.2.0`. The Release workflow uploads signed artifacts, transfers
   the same-IP staging repository to Central Portal, and requests automatic
   validation and release.
5. Confirm the deployment succeeds in Central Portal, then verify dependency
   resolution from Maven Central before marking the GitHub release complete.

For an authorized local release, run `./scripts/publish-maven-central.sh` from
a clean `main` checkout. On macOS, it stores credentials in the login Keychain
after the first secure prompt and requires an explicit version confirmation
before uploading. Use `--forget-credentials` to remove the stored values.

Never place publishing credentials or private signing keys in the repository.
Repository administrators can follow the Chinese
[Maven Central configuration guide](MAVEN_CENTRAL_SETUP.zh-CN.md) to configure
the required GitHub environment and secrets without storing their values in
the repository.
The compatibility upload and same-IP transfer follow Sonatype's
[OSSRH Staging API service documentation](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/).
