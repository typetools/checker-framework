#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
echo Entering $DIR/build.sh

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

if [[ "$TRAVIS" == "true" ]]; then
  SLUGOWNER=${TRAVIS_PULL_REQUEST_SLUG%/*}
  if [[ "$SLUGOWNER" == "" ]]; then
    SLUGOWNER=${TRAVIS_REPO_SLUG%/*}
  fi
elif [ -n "$AZURE_HTTP_USER_AGENT" ]; then
  SLUG=`curl https://api.github.com/repos/${BUILD_REPOSITORY_NAME}/pulls/${SYSTEM_PULLREQUEST_PULLREQUESTNUMBER} | jq .head.label`
  echo "SLUG=$SLUG"
  SLUGOWNER=${SLUG%:*}  # will drop part of string from last occur of `SubStr` to the end
  SLUGREPO=${SLUG#*:}  # will drop begin of string up to first occur of `SubStr`
fi
if [[ "$SLUGOWNER" == "" ]]; then
  SLUGOWNER=typetools
fi
echo SLUGOWNER=$SLUGOWNER

## Build annotation-tools (Annotation File Utilities)
if [ -d ../annotation-tools ] ; then
    git -C ../annotation-tools pull -q || true
else
    [ -d /tmp/plume-scripts ] || (cd /tmp && git clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git)
    REPO=`/tmp/plume-scripts/git-find-fork ${SLUGOWNER} typetools annotation-tools`
    BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${TRAVIS_PULL_REQUEST_BRANCH:-$TRAVIS_BRANCH}`
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
    REPO=`/tmp/plume-scripts/git-find-fork ${SLUGOWNER} typetools stubparser`
    BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${TRAVIS_PULL_REQUEST_BRANCH:-$TRAVIS_BRANCH}`
    (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO}) || (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO})
fi

echo "Running:  (cd ../stubparser/ && ./.travis-build-without-test.sh)"
(cd ../stubparser/ && ./.travis-build-without-test.sh)
echo "... done: (cd ../stubparser/ && ./.travis-build-without-test.sh)"


## Compile

# Two options: rebuild the JDK or download a prebuilt JDK.
if [[ "${BUILDJDK}" == "buildjdk" ]]; then
  echo "running \"./gradlew assemble -PuseLocalJdk\" for checker-framework"
  ./gradlew assemble -PuseLocalJdk --console=plain --warning-mode=all -s --no-daemon
fi

if [[ "${BUILDJDK}" == "downloadjdk" ]]; then
  echo "running \"./gradlew assemble\" for checker-framework"
  ./gradlew assemble --console=plain --warning-mode=all -s --no-daemon
fi

echo Exiting $DIR/build.sh
