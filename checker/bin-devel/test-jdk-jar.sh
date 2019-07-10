#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS

git -C /tmp/plume-scripts pull > /dev/null 2>&1 \
  || git -C /tmp clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git

SLUGOWNER=`/tmp/plume-scripts/git-organization typetools`
echo SLUGOWNER=$SLUGOWNER

export CHECKERFRAMEWORK=`readlink -f ${CHECKERFRAMEWORK:-.}`
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source $SCRIPTDIR/build.sh ${BUILDJDK}


## Run the tests for the type systems that use the annotated JDK
./gradlew IndexTest LockTest NullnessFbcTest OptionalTest printJdkJarManifest -PuseLocalJdk --console=plain --warning-mode=all --no-daemon
