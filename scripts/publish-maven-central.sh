#!/usr/bin/env bash
set -Eeuo pipefail

readonly GPG_FINGERPRINT="25E3D5199FE43B0E9687146450798EB0B0167CC9"
readonly CENTRAL_NAMESPACE="com.owlvigil"
readonly KEYCHAIN_USERNAME_SERVICE="com.owlvigil.maven-central.username"
readonly KEYCHAIN_TOKEN_SERVICE="com.owlvigil.maven-central.token"
readonly KEYCHAIN_GPG_SERVICE="com.owlvigil.maven-central.gpg"

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

usage() {
  cat <<'EOF'
Usage: ./scripts/publish-maven-central.sh [--forget-credentials]

With no arguments, credentials are read from the macOS login Keychain. Missing
credentials are requested once and stored there. --forget-credentials removes
the stored Central token and GPG password without publishing.
EOF
}

forget_credentials() {
  local username=""
  username="$(security find-generic-password -a owlvigil -s "$KEYCHAIN_USERNAME_SERVICE" -w 2>/dev/null || true)"
  if [[ -n "$username" ]]; then
    security delete-generic-password -a "$username" -s "$KEYCHAIN_TOKEN_SERVICE" >/dev/null 2>&1 || true
  fi
  security delete-generic-password -a "$GPG_FINGERPRINT" -s "$KEYCHAIN_GPG_SERVICE" >/dev/null 2>&1 || true
  security delete-generic-password -a owlvigil -s "$KEYCHAIN_USERNAME_SERVICE" >/dev/null 2>&1 || true
  printf 'Stored Maven Central publishing credentials were removed.\n'
}

load_credentials() {
  MAVEN_CENTRAL_USERNAME="$(security find-generic-password -a owlvigil -s "$KEYCHAIN_USERNAME_SERVICE" -w 2>/dev/null || true)"
  if [[ -n "$MAVEN_CENTRAL_USERNAME" ]]; then
    MAVEN_CENTRAL_PASSWORD="$(security find-generic-password -a "$MAVEN_CENTRAL_USERNAME" -s "$KEYCHAIN_TOKEN_SERVICE" -w 2>/dev/null || true)"
  fi
  GPG_PRIVATE_KEY_PASSWORD="$(security find-generic-password -a "$GPG_FINGERPRINT" -s "$KEYCHAIN_GPG_SERVICE" -w 2>/dev/null || true)"
}

store_missing_credentials() {
  if [[ -z "$MAVEN_CENTRAL_USERNAME" || -z "$MAVEN_CENTRAL_PASSWORD" ]]; then
    printf 'Central Token Username: '
    IFS= read -r MAVEN_CENTRAL_USERNAME
    [[ -n "$MAVEN_CENTRAL_USERNAME" ]] || fail "Central Token Username is empty"
    security add-generic-password -U -a owlvigil -s "$KEYCHAIN_USERNAME_SERVICE" \
      -l "OwlVigil Maven Central token username" -w "$MAVEN_CENTRAL_USERNAME" >/dev/null
    printf 'Enter the Central Token Password in the secure Keychain prompt.\n'
    security add-generic-password -U -a "$MAVEN_CENTRAL_USERNAME" -s "$KEYCHAIN_TOKEN_SERVICE" \
      -l "OwlVigil Maven Central token" -w
  fi

  if [[ -z "$GPG_PRIVATE_KEY_PASSWORD" ]]; then
    printf 'Enter the GPG private-key password in the secure Keychain prompt.\n'
    security add-generic-password -U -a "$GPG_FINGERPRINT" -s "$KEYCHAIN_GPG_SERVICE" \
      -l "OwlVigil Maven Central GPG password" -w
  fi

  load_credentials
}

[[ -t 0 ]] || fail "run this script from an interactive terminal"
case "${1:-}" in
  "") ;;
  --forget-credentials)
    command -v security >/dev/null || fail "macOS Keychain command 'security' is required"
    forget_credentials
    exit 0
    ;;
  --help|-h)
    usage
    exit 0
    ;;
  *)
    usage >&2
    fail "unknown argument: $1"
    ;;
esac

command -v gpg >/dev/null || fail "gpg is required"
command -v security >/dev/null || fail "macOS Keychain command 'security' is required"
[[ -x ./gradlew ]] || fail "run this script from the repository root"
[[ "$(git branch --show-current)" == "main" ]] || fail "releases must run from main"
[[ -z "$(git status --porcelain)" ]] || fail "commit or stash all changes before publishing"

version="$(./gradlew -q properties | sed -n 's/^version: //p')"
[[ "$version" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]] ||
  fail "project version is not a stable semantic version: $version"

load_credentials
store_missing_credentials

export MAVEN_CENTRAL_USERNAME MAVEN_CENTRAL_PASSWORD GPG_PRIVATE_KEY_PASSWORD
GPG_PRIVATE_KEY="$(gpg --batch --armor --export-secret-keys "$GPG_FINGERPRINT")"
export GPG_PRIVATE_KEY

[[ -n "$MAVEN_CENTRAL_USERNAME" ]] || fail "Central Token Username is empty"
[[ -n "$MAVEN_CENTRAL_PASSWORD" ]] || fail "Central Token Password is empty"
[[ -n "$GPG_PRIVATE_KEY_PASSWORD" ]] || fail "GPG password is empty"
[[ "$GPG_PRIVATE_KEY" == *"BEGIN PGP PRIVATE KEY BLOCK"* ]] ||
  fail "the configured GPG private key was not found"

printf '\nBuilding and testing %s:%s:%s...\n' "$CENTRAL_NAMESPACE" "owlvigil-java" "$version"
if ! ./gradlew clean test jar sourcesJar javadocJar generatePomFileForMavenJavaPublication \
  signMavenJavaPublication --no-daemon; then
  fail "local build or GPG signing failed; no artifacts were uploaded"
fi

printf '\nThis permanently publishes %s:owlvigil-java:%s to Maven Central.\n' \
  "$CENTRAL_NAMESPACE" "$version"
printf 'Type "yes" to continue: '
IFS= read -r confirmation
confirmation="${confirmation%$'\r'}"
[[ "$confirmation" == "yes" ]] || fail "publication cancelled"

./gradlew publishMavenJavaPublicationToMavenCentralRepository --no-daemon

central_auth="$(printf '%s:%s' "$MAVEN_CENTRAL_USERNAME" "$MAVEN_CENTRAL_PASSWORD" |
  base64 | tr -d '\n')"
curl --fail-with-body --silent --show-error --request POST \
  --header "Authorization: Bearer $central_auth" \
  "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/${CENTRAL_NAMESPACE}?publishing_type=automatic"

printf '\nUpload transferred to Central Portal for automatic validation and release.\n'
