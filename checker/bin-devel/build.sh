#!/bin/bash

echo Entering checker/bin-devel/build.sh in "$(pwd)"
echo checker/bin-devel/build.sh is deprecated: use ./gradlew assemble instead.

# Fail the whole script if any command fails
set -e

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
source "$SCRIPT_DIR"/clone-related.sh

echo "running \"./gradlew assemble\" for checker-framework"
./gradlew assemble -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

echo Exiting checker/bin-devel/build.sh in "$(pwd)"
