#!/bin/bash

echo Deprecated: use ./gradlew assemble instead.
echo Entering checker/bin-devel/build.sh in "$(pwd)"

# Fail the whole script if any command fails
set -e

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
source "$SCRIPT_DIR"/clone-related.sh
IS_CI="$("$SCRIPT_DIR"/is-ci.sh)"

echo "running \"./gradlew assemble\" for checker-framework"
# shellcheck disable=SC2086
./gradlew ${IS_CI:+"--no-daemon"} assemble --console=plain -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

echo Exiting checker/bin-devel/build.sh in "$(pwd)"
