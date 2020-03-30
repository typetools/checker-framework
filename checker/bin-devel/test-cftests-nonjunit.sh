#!/bin/bash

# This script test-cftests-all.sh = tests-cftests-junit.sh + tests-cftests-nonjunit.sh .

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
echo "BUILDJDK=${BUILDJDK}"
# In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
# shellcheck disable=SC1090
source "$SCRIPTDIR"/build.sh "${BUILDJDK}"



./gradlew nonJunitTests --console=plain --warning-mode=all --no-daemon
# Moved example-tests-nobuildjdk out of all tests because it fails in
# the release script because the newest maven artifacts are not published yet.
./gradlew :checker:exampleTests --console=plain --warning-mode=all --no-daemon
