#!/bin/bash

echo Entering "$(cd "$(dirname "$0")" && pwd -P)/$(basename "$0")" in `pwd`

# Optional argument $1 is one of:
#   all, all-tests, junit-tests, nonjunit-tests, jdk.jar, misc, checker-framework-inference, plume-lib, downstream
# It defaults to "all".
export GROUP=$1
if [[ "${GROUP}" == "" ]]; then
  export GROUP=all
fi

if [[ "${GROUP}" != "all" && "${GROUP}" != "all-tests" && "${GROUP}" != "junit-tests" && "${GROUP}" != "nonjunit-tests" && "${GROUP}" != "jdk.jar" && "${GROUP}" != "checker-framework-inference" && "${GROUP}" != "downstream" && "${GROUP}" != "misc" && "${GROUP}" != "plume-lib" ]]; then
  echo "Bad argument '${GROUP}'; should be omitted or one of: all, all-tests, junit-tests, nonjunit-tests, jdk.jar, checker-framework-inference, downstream, misc, plume-lib."
  exit 1
fi

# Optional argument $2 is one of:
#  downloadjdk, buildjdk
# If it is omitted, this script uses downloadjdk.
export BUILDJDK=$2
if [[ "${BUILDJDK}" == "" ]]; then
  export BUILDJDK=buildjdk
fi

if [[ "${BUILDJDK}" != "buildjdk" && "${BUILDJDK}" != "downloadjdk" ]]; then
  echo "Bad argument '${BUILDJDK}'; should be omitted or one of: downloadjdk, buildjdk."
  exit 1
fi

# Fail the whole script if any command fails
set -e

## Diagnostic output
# Output lines of this script as they are read.
set -o verbose
# Output expanded lines of this script as they are executed.
set -o xtrace

export SHELLOPTS

if [ -d "/tmp/plume-scripts" ] ; then
  (cd /tmp/plume-scripts && git pull -q) > /dev/null 2>&1
else
  (cd /tmp && git clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git)
fi
# For debugging
/tmp/plume-scripts/ci-info typetools
eval `/tmp/plume-scripts/ci-info typetools`

export CHECKERFRAMEWORK="${CHECKERFRAMEWORK:-$(pwd -P)}"
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

ROOTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
SCRIPTDIR=$ROOTDIR/checker/bin-devel/

source $SCRIPTDIR/build.sh ${BUILDJDK}
# The above command builds or downloads the JDK, so there is no need for a
# subsequent command to build it except to test building it.

set -e

echo "In checker-framework/.travis-build.sh GROUP=$GROUP"

### TESTS OF THIS REPOSITORY

if [[ "${GROUP}" == "all-tests" || "${GROUP}" == "all" ]]; then
  $SCRIPTDIR/test-all-tests.sh
fi

if [[ "${GROUP}" == "junit-tests" || "${GROUP}" == "all" ]]; then
  $SCRIPTDIR/test-junit-tests.sh
fi

if [[ "${GROUP}" == "nonjunit-tests" || "${GROUP}" == "all" ]]; then
  $SCRIPTDIR/test-nonjunit-tests.sh
fi

if [[ "${GROUP}" == "jdk.jar" || "${GROUP}" == "all" ]]; then
  $SCRIPTDIR/test-jdk-jar.sh
fi

if [[ "${GROUP}" == "misc" || "${GROUP}" == "all" ]]; then
  $SCRIPTDIR/test-misc.sh
fi

### TESTS OF DOWNSTREAM REPOSITORIES

if [[ "${GROUP}" == "checker-framework-inference" || "${GROUP}" == "all" ]]; then
  $SCRIPTDIR/test-cf-inference.sh
fi

if [[ "${GROUP}" == "plume-lib" || "${GROUP}" == "all" ]]; then
  if [ $(java -version 2>&1 | grep version | grep 1.8) -eq 0 ]; then
    $SCRIPTDIR/test-plume-lib.sh
  else
    $SCRIPTDIR/test-plume-lib.sh allJdk11
  fi
fi

if [[ "${GROUP}" == "downstream" || "${GROUP}" == "all" ]]; then
  $SCRIPTDIR/test-downstream.sh
fi

echo Exiting "$(cd "$(dirname "$0")" && pwd -P)/$(basename "$0")" in `pwd`
