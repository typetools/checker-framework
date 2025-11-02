#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

source "$SCRIPT_DIR"/clone-related.sh

# `./gradlew test` subsumes the other commands, but performing them
# separately seems to avoid some out-of-memory errors.
./gradlew assemble --warning-mode=all
./gradlew compileTestJava testClasses --warning-mode=all

## Split "test" into its parts (up to date as of 2025-11-02).
## As of 2025-11-02, :checker:test took 11.5m and :framework:test took 6m.
# ./gradlew test --warning-mode=all
./gradlew :annotation-file-utilities:test --warning-mode=all
./gradlew :checker-qual-android:test --warning-mode=all
./gradlew :checker-qual:test --warning-mode=all
./gradlew :checker-util:test --warning-mode=all
./gradlew :checker:test --warning-mode=all
./gradlew :dataflow:test --warning-mode=all
./gradlew :framework-test:test --warning-mode=all
./gradlew :framework:test --warning-mode=all
./gradlew :javacutil:test --warning-mode=all
./gradlew :test --warning-mode=all
