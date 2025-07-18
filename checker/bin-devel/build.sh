#!/bin/bash

echo Deprecated: use ./gradlew assemble instead.
echo Entering checker/bin-devel/build.sh in "$(pwd)"

# Fail the whole script if any command fails
set -e

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
source "$SCRIPT_DIR"/clone-related.sh
# Download Gradle and dependencies, retrying in case of network problems.
# echo "NO_WRITE_VERIFICATION_METADATA=$NO_WRITE_VERIFICATION_METADATA"
if [ -z "${NO_WRITE_VERIFICATION_METADATA+x}" ]; then
  (date && TERM=dumb timeout 300 ./gradlew --write-verification-metadata sha256 help --dry-run < /dev/null > /dev/null 2>&1) \
    || (sleep 1m && date && TERM=dumb timeout 300 ./gradlew --write-verification-metadata sha256 help --dry-run)
fi
echo "running \"./gradlew assemble\" for checker-framework"
./gradlew assemble --console=plain -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

echo Exiting checker/bin-devel/build.sh in "$(pwd)"
