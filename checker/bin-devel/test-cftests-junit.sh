#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

source "$SCRIPT_DIR"/clone-related.sh

# `./gradlew test` subsumes `./gradlew assemble`, but performing the
# two steps separately seems to avoid out-of-memory errors.
./gradlew assemble --warning-mode=all
./gradlew test --warning-mode=all
