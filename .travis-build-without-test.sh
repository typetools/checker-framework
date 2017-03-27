#!/bin/bash

# Fail the whole script if any command fails
set -e

export SHELLOPTS

SLUGOWNER=${TRAVIS_REPO_SLUG%/*}

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
        SLUGOWNER=typetools
    fi
    set -e
    echo "Running:  (cd .. && git clone --depth 1 https://github.com/${SLUGOWNER}/annotation-tools.git)"
    (cd .. && git clone --depth 1 https://github.com/${SLUGOWNER}/annotation-tools.git)
    echo "... done: (cd .. && git clone --depth 1 https://github.com/${SLUGOWNER}/annotation-tools.git)"
fi

# This also builds jsr308-langtools
echo "Running:  (cd ../annotation-tools/ && ./.travis-build-without-test.sh)"
(cd ../annotation-tools/ && ./.travis-build-without-test.sh)
echo "... done: (cd ../annotation-tools/ && ./.travis-build-without-test.sh)"

## Compile
# Two options: rebuild the JDK or download a prebuilt JDK.  Comment out one.
# If downloading, then some Travis jobs can be combined to save overall time,
# such as merging "demos" into either "nonjunit" or "downstream".  (When
# rebuilding, merging "demos" into one of those could exceed timeouts.)
## To rebuild the JDK:
echo "running \"ant dist\" for checker-framework"
ant dist
## To download a prebuilt JDK:
# echo "running \"ant dist-downloadjdk\" for checker-framework"
# (cd checker && ant dist-downloadjdk)
