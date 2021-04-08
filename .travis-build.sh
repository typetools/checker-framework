#!/bin/bash

echo "Entering $(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd -P)/$(basename "$0") in $(pwd)"


# Fail the whole script if any command fails
set -e

## Diagnostic output
# Output lines of this script as they are read.
set -o verbose
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

ROOTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
SCRIPTDIR=$ROOTDIR/checker/bin-devel/

# For debugging
(cd "$CHECKERFRAMEWORK" && ./gradlew getPlumeScripts -q)
"${SCRIPTDIR}/plume-scripts/ci-info" typetools
eval $("${SCRIPTDIR}/plume-scripts/ci-info" typetools)

source "$SCRIPTDIR/build.sh"

###
### Run the test
###

echo "In checker-framework/.travis-build.sh GROUP=$GROUP"

### TESTS OF THIS REPOSITORY

case  $GROUP  in
    all)
        # Run cftests-junit, cftests-nonjunit, and cftests-inference separately,
        # because cftests-all it takes too long to run on Travis under JDK 11.
        "$SCRIPTDIR/test-cftests-junit.sh"
        "$SCRIPTDIR/test-cftests-nonjunit.sh"
        "$SCRIPTDIR/test-cftests-inference.sh"
        "$SCRIPTDIR/test-misc.sh"
        "$SCRIPTDIR/test-plume-lib.sh"
        "$SCRIPTDIR/test-daikon.sh"
        "$SCRIPTDIR/test-guava.sh"
        "$SCRIPTDIR/test-downstream.sh"
        ;;
    *)
        "${SCRIPTDIR}/test-${GROUP}.sh"
esac


echo Exiting "$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd -P)/$(basename "$0") in $(pwd)"
