#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS

export CHECKERFRAMEWORK=`readlink -f ${CHECKERFRAMEWORK:-.}`
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source $SCRIPTDIR/build.sh ${BUILDJDK}


## Run the tests for the type systems that use the annotated JDK
./gradlew IndexTest LockTest NullnessFbcTest OptionalTest -PuseLocalJdk --console=plain --warning-mode=all --no-daemon
