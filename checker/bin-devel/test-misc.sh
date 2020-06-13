#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
# In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
# shellcheck disable=SC1090
source "$SCRIPTDIR"/build.sh


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

(./gradlew requireJavadoc --console=plain --warning-mode=all --no-daemon > /tmp/warnings-rjp.txt 2>&1) || true
/tmp/"$USER"/plume-scripts/ci-lint-diff /tmp/warnings-rjp.txt

(./gradlew javadocDoclintAll --console=plain --warning-mode=all --no-daemon > /tmp/warnings-jda.txt 2>&1) || true
/tmp/"$USER"/plume-scripts/ci-lint-diff /tmp/warnings-jda.txt
