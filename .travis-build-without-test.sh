#!/bin/bash

# Fail the whole script if any command fails
set -e

# Optional argument $1 is one of:
#  downloadjdk, buildjdk
# If it is omitted, this script uses downloadjdk.
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
    # Older versions of git don't support the -C command-line option
    echo "Running: (cd ../annotation-tools && git pull)"
    (cd ../annotation-tools && git pull)
    echo "... done: (cd ../annotation-tools && git pull)"
else
    set +e
    echo "Running: git ls-remote https://github.com/${SLUGOWNER}/annotation-tools.git &>-"
    git ls-remote https://github.com/${SLUGOWNER}/annotation-tools.git &>-
    if [ "$?" -ne 0 ]; then
        ATSLUGOWNER=typetools
    else
        ATSLUGOWNER=${SLUGOWNER}
    fi
    set -e
    echo "Running:  (cd .. && git clone --depth 1 https://github.com/${ATSLUGOWNER}/annotation-tools.git)"
    (cd .. && git clone --depth 1 https://github.com/${ATSLUGOWNER}/annotation-tools.git)
    echo "... done: (cd .. && git clone --depth 1 https://github.com/${ATSLUGOWNER}/annotation-tools.git)"
fi

# This also builds jsr308-langtools
echo "Running:  (cd ../annotation-tools/ && ./.travis-build-without-test.sh)"
(cd ../annotation-tools/ && ./.travis-build-without-test.sh)
echo "... done: (cd ../annotation-tools/ && ./.travis-build-without-test.sh)"

## Compile
# Two options: rebuild the JDK or download a prebuilt JDK.
if [[ "${BUILDJDK}" == "buildjdk" ]]; then
  echo "running \"ant dist\" for checker-framework"
  ant dist
fi

if [[ "${BUILDJDK}" == "downloadjdk" ]]; then
  echo "running \"ant dist-downloadjdk\" for checker-framework"
  (cd checker && ant dist-downloadjdk)
fi
