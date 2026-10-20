#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

source "$SCRIPT_DIR"/clone-related.sh

# Pluggable type-checking:  run the Checker Framework on itself.
# `--continue` reports every type-checking failure, not just the first one.
gradle_retry_once typecheck-part1 --continue --warning-mode=all
