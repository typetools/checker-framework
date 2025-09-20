#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

source "$SCRIPT_DIR"/clone-related.sh

./gradlew getPlumeScripts
PLUME_SCRIPTS="$SCRIPT_DIR/.plume-scripts"

# Pluggable type-checking:  run the Checker Framework on itself
./gradlew typecheck-part2 --console=plain --warning-mode=all

if [ -f SKIP-REQUIRE-JAVADOC ]; then
  echo "Skipping checkNullness because file SKIP-REQUIRE-JAVADOC exists."
else
  (./gradlew checkNullness -PnullnessAll --console=plain --warning-mode=all > /tmp/warnings-checkNullness.txt 2>&1) || true
  "$PLUME_SCRIPTS"/ci-lint-diff /tmp/warnings-checkNullness.txt
fi
