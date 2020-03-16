#!/bin/bash

echo "Entering $(cd "$(dirname "$0")" && pwd -P)/$(basename "$0") in $(pwd)"


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

# Optional argument $2 is one of:
#  downloadjdk, buildjdk
# It defaults to downloadjdk.
export BUILDJDK=$2
if [[ "${BUILDJDK}" == "" ]]; then
  export BUILDJDK=buildjdk
fi

if [[ "${BUILDJDK}" != "buildjdk" && "${BUILDJDK}" != "downloadjdk" ]]; then
  echo "Bad argument '${BUILDJDK}'; should be omitted or one of: downloadjdk, buildjdk."
  exit 1
fi


###
### Build the Checker Framework
###

if [ -d "/tmp/plume-scripts" ] ; then
  (cd /tmp/plume-scripts && git pull -q)
else
  (cd /tmp && git clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git)
fi
# For debugging
/tmp/plume-scripts/ci-info typetools
eval $(/tmp/plume-scripts/ci-info typetools)

export CHECKERFRAMEWORK="${CHECKERFRAMEWORK:-$(pwd -P)}"
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

ROOTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
SCRIPTDIR=$ROOTDIR/checker/bin-devel/

source "$SCRIPTDIR/build.sh" "${BUILDJDK}"
# The above command builds or downloads the JDK, so there is no need for a
# subsequent command to build it except to test building it.


###
### Run the test
###

echo "In checker-framework/.travis-build.sh GROUP=$GROUP"

### TESTS OF THIS REPOSITORY

case  $GROUP  in
    all)
        # Run cftests-junit and cftests-nonjunit separately, because cftests-all it takes too long to run on Travis under JDK 11.
        "$SCRIPTDIR/test-cftests-junit.sh"
        "$SCRIPTDIR/test-cftests-nonjunit.sh"
        "$SCRIPTDIR/test-jdk-jar.sh"
        "$SCRIPTDIR/test-misc.sh"
        "$SCRIPTDIR/test-cf-inference.sh"
        "$SCRIPTDIR/test-plume-lib.sh"
        "$SCRIPTDIR/test-daikon.sh"
        "$SCRIPTDIR/test-guava.sh"
        "$SCRIPTDIR/test-downstream.sh"
        ;;
    *)
        "${SCRIPTDIR}/test-${GROUP}.sh"
esac


echo Exiting "$(cd "$(dirname "$0")" && pwd -P)/$(basename "$0") in $(pwd)"
