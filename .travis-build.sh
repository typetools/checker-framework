#!/bin/bash

echo "Entering $(cd "$(dirname "$0")" > /dev/null 2>&1 && pwd -P)/$(basename "$0") in $(pwd)"

# Fail the whole script if any command fails
set -e

## Diagnostic output
# Output lines of this script as they are read.
# set -o verbose
# Output expanded lines of this script as they are executed.
set -o xtrace

export SHELLOPTS

###
### Argument parsing
###

# Optional argument $1 defaults to "all".
export GROUP=$1
if [[ "${GROUP}" == "" ]]; then
  export GROUP=all
fi

###
### Build the Checker Framework
###

export CHECKERFRAMEWORK="${CHECKERFRAMEWORK:-$(pwd -P)}"
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
BIN_DEVEL_DIR="${SCRIPT_DIR}/checker/bin-devel/"

# For debugging
(cd "$CHECKERFRAMEWORK" && ./gradlew getPlumeScripts)
"${BIN_DEVEL_DIR}/plume-scripts/ci-info" typetools
eval "$("${BIN_DEVEL_DIR}/plume-scripts/ci-info" typetools)"

source "$BIN_DEVEL_DIR/checker/bin-devel/clone-related.sh"

###
### Run the test
###

echo "In checker-framework/.travis-build.sh GROUP=$GROUP"

### TESTS OF THIS REPOSITORY

case $GROUP in
  all)
    # Run cftests-junit, cftests-nonjunit, and cftests-inference separately,
    # because cftests-all takes too long to run on Travis.
    "$BIN_DEVEL_DIR/test-cftests-junit.sh"
    "$BIN_DEVEL_DIR/test-cftests-nonjunit.sh"
    "$BIN_DEVEL_DIR/test-cftests-inference.sh"
    "$BIN_DEVEL_DIR/test-misc.sh"
    "$BIN_DEVEL_DIR/test-typecheck.sh"
    "$BIN_DEVEL_DIR/test-plume-lib.sh"
    "$BIN_DEVEL_DIR/test-daikon.sh"
    "$BIN_DEVEL_DIR/test-guava.sh"
    "$BIN_DEVEL_DIR/test-downstream.sh"
    ;;
  *)
    "${BIN_DEVEL_DIR}/test-${GROUP}.sh"
    ;;
esac

echo Exiting "$(cd "$(dirname "$0")" > /dev/null 2>&1 && pwd -P)/$(basename "$0") in $(pwd)"
