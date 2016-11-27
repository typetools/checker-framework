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
    if [ "$?" -eq 0 ]; then
        echo "Running:  (cd .. && git clone --depth 1 https://github.com/${SLUGOWNER}/annotation-tools.git)"
        (cd .. && git clone --depth 1 https://github.com/${SLUGOWNER}/annotation-tools.git)
        echo "... done: (cd .. && git clone --depth 1 https://github.com/${SLUGOWNER}/annotation-tools.git)"
    else
        echo "Running: (cd .. && git clone --depth 1 https://github.com/typetools/annotation-tools.git)"
        (cd .. && git clone --depth 1 https://github.com/typetools/annotation-tools.git)
        echo "... done: (cd .. && git clone --depth 1 https://github.com/typetools/annotation-tools.git)"
    fi
    set -e
fi

# This also builds jsr308-langtools
echo "Running:  (cd ../annotation-tools/ && ./.travis-build-without-test.sh)"
(cd ../annotation-tools/ && ./.travis-build-without-test.sh)
echo "... done: (cd ../annotation-tools/ && ./.travis-build-without-test.sh)"

## Compile
echo "running \"ant dist\" for checker-framework"
ant dist
