#!/bin/bash

# This script test-cftests-all.sh = tests-cftests-junit.sh + tests-cftests-nonjunit.sh + tests-cftests-inference.sh + tests-typecheck.sh .
# Per comments in ../../build.gradle, allTests = test + nonJunitTests + inferenceTests + typecheck .

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

export ORG_GRADLE_PROJECT_useJdk21Compiler=true
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
source "$SCRIPT_DIR"/clone-related.sh

./gradlew allTests --console=plain --warning-mode=all
# Moved example-tests out of all tests because it fails in
# the release script because the newest maven artifacts are not published yet.
./gradlew :checker:exampleTests --console=plain --warning-mode=all
