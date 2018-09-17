#!/bin/bash

echo Entering `pwd`/.travis-build-without-test.sh

# Fail the whole script if any command fails
set -e

# Optional argument $1 is one of:
#  downloadjdk, buildjdk
# If it is omitted, this script uses buildjdk.
export BUILDJDK=$1
if [[ "${BUILDJDK}" == "" ]]; then
  export BUILDJDK=buildjdk
fi

if [[ "${BUILDJDK}" != "buildjdk" && "${BUILDJDK}" != "downloadjdk" ]]; then
  echo "Bad argument '${BUILDJDK}'; should be omitted or one of: downloadjdk, buildjdk."
  exit 1
fi

export SHELLOPTS

SLUGOWNER=${TRAVIS_REPO_SLUG%/*}
if [[ "$SLUGOWNER" == "" ]]; then
  SLUGOWNER=typetools
fi


## Build annotation-tools (Annotation File Utilities)
if [ -d ../annotation-tools ] ; then
    git -C ../annotation-tools pull || true
else
    [ -d /tmp/plume-scripts ] || (cd /tmp && git clone --depth 1 https://github.com/plume-lib/plume-scripts.git)
    REPO=`/tmp/plume-scripts/git-find-fork ${SLUGOWNER} typetools annotation-tools`
    BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${TRAVIS_PULL_REQUEST_BRANCH:-$TRAVIS_BRANCH}`
    (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 ${REPO}) || (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 ${REPO})
fi

echo "Running:  (cd ../annotation-tools/ && ./.travis-build-without-test.sh)"
(cd ../annotation-tools/ && ./.travis-build-without-test.sh)
echo "... done: (cd ../annotation-tools/ && ./.travis-build-without-test.sh)"


## Build stubparser
if [ -d ../stubparser ] ; then
    git -C ../stubparser pull
else
    [ -d /tmp/plume-scripts ] || (cd /tmp && git clone --depth 1 https://github.com/plume-lib/plume-scripts.git)
    REPO=`/tmp/plume-scripts/git-find-fork ${SLUGOWNER} typetools stubparser`
    BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${TRAVIS_PULL_REQUEST_BRANCH:-$TRAVIS_BRANCH}`
    (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 ${REPO}) || (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 ${REPO})
fi

echo "Running:  (cd ../stubparser/ && ./.travis-build-without-test.sh)"
(cd ../stubparser/ && ./.travis-build-without-test.sh)
echo "... done: (cd ../stubparser/ && ./.travis-build-without-test.sh)"


## Compile

# Download jsr308-langtools replacement for javac.jar that fixes some bugs
if [ ! -d ../jsr308-langtools ] ; then
  (cd .. && wget -q https://checkerframework.org/jsr308/jsr308-langtools-2.4.0.zip)
  (cd .. && unzip -q jsr308-langtools-2.4.0.zip)
  (cd .. && mv jsr308-langtools-2.4.0 jsr308-langtools)
fi

# Two options: rebuild the JDK or download a prebuilt JDK.
if [[ "${BUILDJDK}" == "buildjdk" ]]; then
  echo "running \"./gradlew assemble -PuseLocalJdk\" for checker-framework"
   ./gradlew assemble -PuseLocalJdk --console=plain --warning-mode=all -s
fi

if [[ "${BUILDJDK}" == "downloadjdk" ]]; then
  echo "running \"./gradlew assemble\" for checker-framework"
  ./gradlew assemble --console=plain --warning-mode=all -s
fi

echo Exiting `pwd`/.travis-build-without-test.sh
