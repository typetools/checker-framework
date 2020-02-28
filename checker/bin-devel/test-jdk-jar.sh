#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

export CHECKERFRAMEWORK="${CHECKERFRAMEWORK:-$(pwd -P)}"
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
echo "BUILDJDK=${BUILDJDK}"
# In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
# shellcheck disable=SC1090
source "$SCRIPTDIR"/build.sh "${BUILDJDK}"


## Run the tests for the type systems that use the annotated JDK
./gradlew IndexTest LockTest NullnessFbcTest OptionalTest -PuseLocalJdk --console=plain --warning-mode=all --no-daemon
