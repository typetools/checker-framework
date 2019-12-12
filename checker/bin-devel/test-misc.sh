#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS

git -C /tmp/plume-scripts pull > /dev/null 2>&1 \
  || git -C /tmp clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git

export CHECKERFRAMEWORK="${CHECKERFRAMEWORK:-$(pwd -P)}"
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source $SCRIPTDIR/build.sh ${BUILDJDK}


# Code style and formatting
./gradlew checkBasicStyle checkFormat --console=plain --warning-mode=all --no-daemon

# Run error-prone
./gradlew runErrorProne --console=plain --warning-mode=all --no-daemon

# HTML legality
./gradlew htmlValidate --console=plain --warning-mode=all --no-daemon

# Documentation
./gradlew javadoc --console=plain --warning-mode=all --no-daemon

./gradlew javadocPrivate --console=plain --warning-mode=all --no-daemon
make -C docs/manual all

/tmp/plume-scripts/ci-info --debug
echo "end of /tmp/plume-scripts/ci-info --debug"

(./gradlew requireJavadocPrivate --console=plain --warning-mode=all --no-daemon > /tmp/warnings-rjp.txt 2>&1) || true
/tmp/plume-scripts/ci-lint-diff /tmp/warnings-rjp.txt

(./gradlew javadocDoclintAll --console=plain --warning-mode=all --no-daemon > /tmp/warnings-jda.txt 2>&1) || true
/tmp/plume-scripts/ci-lint-diff /tmp/warnings-jda.txt
