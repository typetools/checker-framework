#!/bin/bash

echo Deprecated: use ./gradlew assemble instead.
echo Entering checker/bin-devel/build.sh in "$(pwd)"

# Fail the whole script if any command fails
set -e

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source "$SCRIPTDIR"/clone-related.sh
# Download dependencies, trying a second time if there is a failure.
# echo "NO_WRITE_VERIFICATION_METADATA=$NO_WRITE_VERIFICATION_METADATA"
if [ -z "${NO_WRITE_VERIFICATION_METADATA+x}" ]; then
(TERM=dumb timeout 300 ./gradlew --write-verification-metadata sha256 help --dry-run || \
     (sleep 1m && TERM=dumb timeout 300 ./gradlew --write-verification-metadata sha256 help --dry-run))
fi
echo "running \"./gradlew assemble\" for checker-framework"
./gradlew assemble --console=plain -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

echo Exiting checker/bin-devel/build.sh in "$(pwd)"
