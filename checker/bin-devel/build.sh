#!/bin/bash

echo Entering checker/bin-devel/build.sh in "$(pwd)"

# Fail the whole script if any command fails
set -e

echo "initial CHECKERFRAMEWORK=$CHECKERFRAMEWORK"
export CHECKERFRAMEWORK="${CHECKERFRAMEWORK:-$(pwd -P)}"
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

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

if [ -d "/tmp/$USER/plume-scripts" ] ; then
  (cd /tmp/$USER/plume-scripts && git pull -q)
else
  mkdir -p /tmp/$USER && (cd /tmp/$USER && (git clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git || git clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git))
fi

# Clone the annotated JDK 11 into ../jdk .
/tmp/$USER/plume-scripts/git-clone-related typetools jdk

AFU="${AFU:-../annotation-tools/annotation-file-utilities}"
# Don't use `AT=${AFU}/..` which causes a git failure.
AT=$(dirname "${AFU}")

## Build annotation-tools (Annotation File Utilities)
/tmp/$USER/plume-scripts/git-clone-related typetools annotation-tools "${AT}"
if [ ! -d ../annotation-tools ] ; then
  ln -s "${AT}" ../annotation-tools
fi

echo "Running:  (cd ${AT} && ./.travis-build-without-test.sh)"
(cd "${AT}" && ./.travis-build-without-test.sh)
echo "... done: (cd ${AT} && ./.travis-build-without-test.sh)"


## Build stubparser
/tmp/$USER/plume-scripts/git-clone-related typetools stubparser
echo "Running:  (cd ../stubparser/ && ./.travis-build-without-test.sh)"
(cd ../stubparser/ && ./.travis-build-without-test.sh)
echo "... done: (cd ../stubparser/ && ./.travis-build-without-test.sh)"


## Compile

# Downloading the gradle wrapper sometimes fails.
# If so, the next command gets another chance to try the download.
(./gradlew help || sleep 10) > /dev/null 2>&1

# Two options: download a prebuilt JDK or rebuild the JDK.
if [[ "${BUILDJDK}" == "downloadjdk" ]]; then
  echo "running \"./gradlew assemble printJdkJarManifest\" for checker-framework"
  ./gradlew assemble printJdkJarManifest --console=plain --warning-mode=all -s --no-daemon -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000
else
  echo "running \"./gradlew assemble -PuseLocalJdk\" for checker-framework"
  ./gradlew assemble -PuseLocalJdk --console=plain --warning-mode=all -s --no-daemon -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000
fi

echo Exiting checker/bin-devel/build.sh in "$(pwd)"
