#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
echo Entering $0

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

JAVA_HOME=${JAVA_HOME:-`which javac|xargs readlink -f|xargs dirname|xargs dirname`}
export JAVA_HOME

git -C /tmp/plume-scripts pull > /dev/null 2>&1 \
    || git -C /tmp clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git
eval `/tmp/plume-scripts/ci-info typetools`


## Build annotation-tools (Annotation File Utilities)
if [ -d ../annotation-tools ] ; then
    git -C ../annotation-tools pull -q || true
else
    [ -d /tmp/plume-scripts ] || (cd /tmp && git clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git)
    REPO=`/tmp/plume-scripts/git-find-fork ${CI_ORGANIZATION} typetools annotation-tools`
    BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${CI_BRANCH}`
    (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO}) || (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO})
fi

echo "Running:  (cd ../annotation-tools/ && ./.travis-build-without-test.sh)"
(cd ../annotation-tools/ && ./.travis-build-without-test.sh)
echo "... done: (cd ../annotation-tools/ && ./.travis-build-without-test.sh)"


## Build stubparser
if [ -d ../stubparser ] ; then
    git -C ../stubparser pull
else
    [ -d /tmp/plume-scripts ] || (cd /tmp && git clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git)
    REPO=`/tmp/plume-scripts/git-find-fork ${CI_ORGANIZATION} typetools stubparser`
    BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${CI_BRANCH}`
    (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO}) || (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO})
fi

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

echo Exiting $0
