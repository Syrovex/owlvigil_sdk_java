#!/usr/bin/env bash
set -Eeuo pipefail

readonly GPG_FINGERPRINT="25E3D5199FE43B0E9687146450798EB0B0167CC9"
readonly CENTRAL_NAMESPACE="com.owlvigil"

cleanup() {
  unset MAVEN_CENTRAL_USERNAME MAVEN_CENTRAL_PASSWORD
  unset GPG_PRIVATE_KEY GPG_PRIVATE_KEY_PASSWORD central_auth
}

handle_signal() {
  local exit_code="$1"
  trap - EXIT INT TERM
  cleanup
  printf '\nPublication cancelled.\n' >&2
  exit "$exit_code"
}

trap cleanup EXIT
trap 'handle_signal 130' INT
trap 'handle_signal 143' TERM

fail() {
  printf 'error: %s\n' "$*" >&2
  exit 1
}

[[ -t 0 ]] || fail "run this script from an interactive terminal"
command -v gpg >/dev/null || fail "gpg is required"
[[ -x ./gradlew ]] || fail "run this script from the repository root"
[[ "$(git branch --show-current)" == "main" ]] || fail "releases must run from main"
[[ -z "$(git status --porcelain)" ]] || fail "commit or stash all changes before publishing"

version="$(./gradlew -q properties | sed -n 's/^version: //p')"
[[ "$version" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]] ||
  fail "project version is not a stable semantic version: $version"

printf 'Central Token Username: '
IFS= read -r -s MAVEN_CENTRAL_USERNAME
printf '\nCentral Token Password: '
IFS= read -r -s MAVEN_CENTRAL_PASSWORD
printf '\nGPG private-key password: '
IFS= read -r -s GPG_PRIVATE_KEY_PASSWORD
printf '\n'

export MAVEN_CENTRAL_USERNAME MAVEN_CENTRAL_PASSWORD GPG_PRIVATE_KEY_PASSWORD
GPG_PRIVATE_KEY="$(gpg --batch --armor --export-secret-keys "$GPG_FINGERPRINT")"
export GPG_PRIVATE_KEY

[[ -n "$MAVEN_CENTRAL_USERNAME" ]] || fail "Central Token Username is empty"
[[ -n "$MAVEN_CENTRAL_PASSWORD" ]] || fail "Central Token Password is empty"
[[ -n "$GPG_PRIVATE_KEY_PASSWORD" ]] || fail "GPG password is empty"
[[ "$GPG_PRIVATE_KEY" == *"BEGIN PGP PRIVATE KEY BLOCK"* ]] ||
  fail "the configured GPG private key was not found"

printf '\nBuilding and testing %s:%s:%s...\n' "$CENTRAL_NAMESPACE" "owlvigil-java" "$version"
./gradlew clean test jar sourcesJar javadocJar generatePomFileForMavenJavaPublication --no-daemon

printf '\nThis permanently publishes %s:owlvigil-java:%s to Maven Central.\n' \
  "$CENTRAL_NAMESPACE" "$version"
printf 'Type "publish %s" to continue: ' "$version"
IFS= read -r confirmation
[[ "$confirmation" == "publish $version" ]] || fail "publication cancelled"

./gradlew publishMavenJavaPublicationToMavenCentralRepository --no-daemon

central_auth="$(printf '%s:%s' "$MAVEN_CENTRAL_USERNAME" "$MAVEN_CENTRAL_PASSWORD" |
  base64 | tr -d '\n')"
curl --fail-with-body --silent --show-error --request POST \
  --header "Authorization: Bearer $central_auth" \
  "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/${CENTRAL_NAMESPACE}?publishing_type=automatic"

printf '\nUpload transferred to Central Portal for automatic validation and release.\n'
