#!/bin/bash

# Fail the whole script if any command fails
set -e

export SHELLOPTS

SLUGOWNER=${TRAVIS_REPO_SLUG%/*}

# Optional argument $2 is one of:
#   jdk7, jdk8
# If omited, then either jdk may be used.
JDKVER=$1

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
#TODO: The docker images should be updated to have wget installed.
apt-get update
apt-get install wget

if [[ "${JDKVER}" == "jdk7" || "${JDKVER}" == "" ]]; then
wget https://checkerframework.org/dev-jdk/jdk7.jar -O checker/jdk/jdk7.jar
# The implementation version listed in the Manifest is the hash of the commit of
# the Checker Framework that created the jdk.jar.  Show it here to help debugging.
jar -xvf checker/jdk/jdk7.jar  META-INF/MANIFEST.MF && cat META-INF/MANIFEST.MF
fi
if [[ "${JDKVER}" == "jdk8" || "${JDKVER}" == "" ]]; then
wget https://checkerframework.org/dev-jdk/jdk8.jar -O checker/jdk/jdk8.jar
jar -xvf checker/jdk/jdk8.jar  META-INF/MANIFEST.MF && cat META-INF/MANIFEST.MF
fi

echo "running \"ant dist-nobuildjdk\" for checker-framework"
(cd checker && ant dist-nobuildjdk)
