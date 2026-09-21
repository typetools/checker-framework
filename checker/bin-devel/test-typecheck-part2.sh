#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

source "$SCRIPT_DIR"/clone-related.sh

"$SCRIPT_DIR"/clone-plume-scripts.sh
PLUME_SCRIPTS="$SCRIPT_DIR/.plume-scripts"

# Pluggable type-checking:  run the Checker Framework on itself.
# `--continue` reports every type-checking failure, not just the first one.
gradle_retry_once typecheck-part2 --continue --warning-mode=all

if [ -f SKIP-REQUIRE-JAVADOC ]; then
  echo "Skipping checkNullness because file SKIP-REQUIRE-JAVADOC exists."
else
  # `--continue` ensures determinism: don't stop after the first parallel task failure.
  (./gradlew checkNullness -PnullnessAll --continue --warning-mode=all > /tmp/warnings-checkNullness.txt 2>&1) || true
  "$PLUME_SCRIPTS"/ci-lint-diff /tmp/warnings-checkNullness.txt
fi
