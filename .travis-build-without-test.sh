#!/bin/bash

# Fail the whole script if any command fails
set -e

export SHELLOPTS

SLUGOWNER=${TRAVIS_REPO_SLUG%/*}

## Build annotation-tools (Annotation File Utilities)
if [ -d ../annotation-tools ] ; then
    # Older versions of git don't support the -C command-line option
    (cd ../annotation-tools && git pull)
else
    (cd .. && git clone --depth 1 https://github.com/${SLUGOWNER}/annotation-tools.git)
fi
# This also builds jsr308-langtools
(cd ../annotation-tools/ && ./.travis-build-without-test.sh)

## Compile
ant dist
