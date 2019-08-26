#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS

export CHECKERFRAMEWORK=`readlink -f ${CHECKERFRAMEWORK:-.}`
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source $SCRIPTDIR/build.sh ${BUILDJDK}



./gradlew allTests --console=plain --warning-mode=all --no-daemon
# Moved example-tests-nobuildjdk out of all tests because it fails in
# the release script because the newest maven artifacts are not published yet.
# This fails because the released version of Checker Framework requires the Java 8 compiler.
# TODO: After 2.11.0 is released, uncomment.
# ./gradlew :checker:exampleTests --console=plain --warning-mode=all --no-daemon
