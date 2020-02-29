#!/bin/bash

echo Entering checker/bin-devel/build.sh in "$(pwd)"

# Fail the whole script if any command fails
set -e

# Optional argument $1 is one of:
#  downloadjdk, buildjdk
# If it is omitted, this script uses downloadjdk.
export BUILDJDK=$1
if [[ "${BUILDJDK}" == "" ]]; then
  export BUILDJDK=downloadjdk
fi
echo "BUILDJDK=${BUILDJDK}"

if [[ "${BUILDJDK}" != "buildjdk" && "${BUILDJDK}" != "downloadjdk" ]]; then
  echo "Bad argument '${BUILDJDK}'; should be omitted or one of: downloadjdk, buildjdk."
  exit 1
fi

export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

if [ "$(uname)" == "Darwin" ] ; then
  export JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home)}
else
  # shellcheck disable=SC2230
  export JAVA_HOME=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(which javac)")")")}
fi
echo "JAVA_HOME=${JAVA_HOME}"

if [ -d "/tmp/plume-scripts" ] ; then
  (cd /tmp/plume-scripts && git pull -q)
else
  (cd /tmp && git clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git)
fi

# Clone the annotated JDK 11 into ../jdk .
/tmp/plume-scripts/git-clone-related typetools jdk

# This does not work:
#   AT=${AFU}/..
# because `git clone REPO ../annotation-tools/annotation-file-utilities/..`
# fails with
#   fatal: could not create work tree dir '../annotation-tools/annotation-file-utilities/..': File exists
#   fatal: destination path '../annotation-tools/annotation-file-utilities/..' already exists and is not an empty directory.
# even if the directory does not exist!
# The reason is that git creates each element of the path:
#  ..
#  ../annotation-tools
#  ../annotation-tools/annotation-file-utilities (this is the problem),
#  ../annotation-tools/annotation-file-utilities/..
#  etc.

AFU="${AFU:-../annotation-tools/annotation-file-utilities}"
AT=$(dirname "${AFU}")

## Build annotation-tools (Annotation File Utilities)
/tmp/plume-scripts/git-clone-related typetools annotation-tools "${AT}"
if [ ! -d ../annotation-tools ] ; then
  ln -s "${AT}" ../annotation-tools
fi

echo "Running:  (cd ${AT} && ./.travis-build-without-test.sh)"
(cd "${AT}" && ./.travis-build-without-test.sh)
echo "... done: (cd ${AT} && ./.travis-build-without-test.sh)"


## Build stubparser
/tmp/plume-scripts/git-clone-related typetools stubparser

echo "Running:  (cd ../stubparser/ && ./.travis-build-without-test.sh)"
(cd ../stubparser/ && ./.travis-build-without-test.sh)
echo "... done: (cd ../stubparser/ && ./.travis-build-without-test.sh)"


## Compile

# Downloading the gradle wrapper sometimes fails.
# If so, the next command gets another chance to try the download.
(./gradlew help || sleep 10) > /dev/null 2>&1

# Two options: download a prebuilt JDK or rebuild the JDK.
if [[ "${BUILDJDK}" == "downloadjdk" ]]; then
  echo "running \"./gradlew assemble\" for checker-framework"
  ./gradlew assemble printJdkJarManifest --console=plain --warning-mode=all -s --no-daemon
else
  echo "running \"./gradlew assemble -PuseLocalJdk\" for checker-framework"
  ./gradlew assemble -PuseLocalJdk --console=plain --warning-mode=all -s --no-daemon
fi

echo Exiting checker/bin-devel/build.sh in "$(pwd)"
