#!/bin/bash

echo Entering "$(cd "$(dirname "$0")" && pwd -P)/$(basename "$0")"

# Fail the whole script if any command fails
set -e

# Optional argument $1 is one of:
#  downloadjdk, buildjdk
# If it is omitted, this script uses downloadjdk.
export BUILDJDK=$1
if [[ "${BUILDJDK}" == "" ]]; then
  export BUILDJDK=downloadjdk
fi

if [[ "${BUILDJDK}" != "buildjdk" && "${BUILDJDK}" != "downloadjdk" ]]; then
  echo "Bad argument '${BUILDJDK}'; should be omitted or one of: downloadjdk, buildjdk."
  exit 1
fi

export SHELLOPTS

if [ "$(uname)" == "Darwin" ] ; then
  export JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home)}
else
  export JAVA_HOME=${JAVA_HOME:-$(dirname $(dirname $(readlink -f $(which javac))))}
fi

git -C /tmp/plume-scripts pull > /dev/null 2>&1 \
  || git -C /tmp clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git

# This does not work:
#   AT=${AFU}/..
# because `git clone REPO ../annotation-tools/annotation-file-utilities/..`
# fails with
#   fatal: could not create work tree dir '../annotation-tools/annotation-file-utilities/..': File exists
#   fatal: destination path '../annotation-tools/annotation-file-utilities/..' already exists and is not an empty directory.
# even if the directory does not exist!
# The reason is that git creates each element of the path:
#  .. , ../annotation-tools, ../annotation-tools/annotation-file-utilities
#  (this is the problem), and../annotation-tools/annotation-file-utilities/.. .

AFU="${AFU:-../annotation-tools/annotation-file-utilities}"
AT=$(dirname "${AFU}")

## Build annotation-tools (Annotation File Utilities)
/tmp/plume-scripts/git-clone-related typetools annotation-tools ${AT}
if [ ! -d ../annotation-tools ] ; then
  ln -s ${AT} ../annotation-tools
fi

echo "Running:  (cd ${AT} && ./.travis-build-without-test.sh)"
(cd ${AT} && ./.travis-build-without-test.sh)
echo "... done: (cd ${AT} && ./.travis-build-without-test.sh)"


## Build stubparser
/tmp/plume-scripts/git-clone-related typetools stubparser

echo "Running:  (cd ../stubparser/ && ./.travis-build-without-test.sh)"
(cd ../stubparser/ && ./.travis-build-without-test.sh)
echo "... done: (cd ../stubparser/ && ./.travis-build-without-test.sh)"


## Compile

# Two options: rebuild the JDK or download a prebuilt JDK.
if [[ "${BUILDJDK}" == "downloadjdk" ]]; then
  echo "running \"./gradlew assemble\" for checker-framework"
  ./gradlew assemble printJdkJarManifest --console=plain --warning-mode=all -s --no-daemon
else
  echo "running \"./gradlew assemble -PuseLocalJdk\" for checker-framework"
  ./gradlew assemble -PuseLocalJdk --console=plain --warning-mode=all -s --no-daemon
fi

echo Exiting "$(cd "$(dirname "$0")" && pwd -P)/$(basename "$0")"
