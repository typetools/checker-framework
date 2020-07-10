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

# Javadoc documentation
# Uncomment this line temporarily for refactorings that touch a lot of code that
# you don't understand.  Then, recomment it as soon as the pull request is merged.
# SKIPJAVADOC=1
if [ -z "$SKIPJAVADOC" ]; then
status=0
./gradlew javadoc --console=plain --warning-mode=all --no-daemon || status=1
./gradlew javadocPrivate --console=plain --warning-mode=all --no-daemon || status=1
(./gradlew requireJavadoc --console=plain --warning-mode=all --no-daemon > /tmp/warnings-rjp.txt 2>&1) || true
/tmp/"$USER"/plume-scripts/ci-lint-diff /tmp/warnings-rjp.txt || status=1
(./gradlew javadocDoclintAll --console=plain --warning-mode=all --no-daemon > /tmp/warnings-jda.txt 2>&1) || true
/tmp/"$USER"/plume-scripts/ci-lint-diff /tmp/warnings-jda.txt || status=1
if [ $status -ne 0 ]; then exit $status; fi
fi # end of "if [ -z $SKIPJAVADOC ]"


# User documentation
make -C docs/manual all
