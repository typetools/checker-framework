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

# Pluggable type-checking
status=0
./gradlew :checker:checkInterning --quiet --console=plain --warning-mode=all --no-daemon
./gradlew :checker-qual:checkInterning --quiet --console=plain --warning-mode=all --no-daemon
./gradlew :dataflow:checkInterning --quiet --console=plain --warning-mode=all --no-daemon
(./gradlew :framework:checkInterning --quiet --console=plain --warning-mode=all --no-daemon > /tmp/warnings-ci.txt 2>&1) || true
/tmp/"$USER"/plume-scripts/ci-lint-diff /tmp/warnings-ci.txt || status=1
./gradlew :framework-test:checkInterning --quiet --console=plain --warning-mode=all
./gradlew :javacutil:checkInterning --quiet --console=plain --warning-mode=all --no-daemon

(./gradlew :checker:checkNullness --quiet --console=plain --warning-mode=all --no-daemon > /tmp/warnings-cn.txt 2>&1) || true
/tmp/"$USER"/plume-scripts/ci-lint-diff /tmp/warnings-cn.txt || status=1
(./gradlew :checker-qual:checkNullness --quiet --console=plain --warning-mode=all --no-daemon > /tmp/warnings-cn.txt 2>&1) || true
/tmp/"$USER"/plume-scripts/ci-lint-diff /tmp/warnings-cn.txt || status=1
(./gradlew :dataflow:checkNullness --quiet --console=plain --warning-mode=all --no-daemon > /tmp/warnings-cn.txt 2>&1) || true
/tmp/"$USER"/plume-scripts/ci-lint-diff /tmp/warnings-cn.txt || status=1
(./gradlew :framework:checkNullness --quiet --console=plain --warning-mode=all --no-daemon > /tmp/warnings-cn.txt 2>&1) || true
/tmp/"$USER"/plume-scripts/ci-lint-diff /tmp/warnings-cn.txt || status=1
(./gradlew :framework-test:checkNullness --quiet --console=plain --warning-mode=all --no-daemon > /tmp/warnings-cn.txt 2>&1) || true
/tmp/"$USER"/plume-scripts/ci-lint-diff /tmp/warnings-cn.txt || status=1
(./gradlew :javacutil:checkNullness --quiet --console=plain --warning-mode=all --no-daemon > /tmp/warnings-cn.txt 2>&1) || true
/tmp/"$USER"/plume-scripts/ci-lint-diff /tmp/warnings-cn.txt || status=1
if [ $status -ne 0 ]; then exit $status; fi

# HTML legality
./gradlew htmlValidate --console=plain --warning-mode=all --no-daemon

# Javadoc documentation
status=0
./gradlew javadoc --console=plain --warning-mode=all --no-daemon || status=1
./gradlew javadocPrivate --console=plain --warning-mode=all --no-daemon || status=1
(./gradlew requireJavadoc --console=plain --warning-mode=all --no-daemon > /tmp/warnings-rjp.txt 2>&1) || true
/tmp/"$USER"/plume-scripts/ci-lint-diff /tmp/warnings-rjp.txt || status=1
(./gradlew javadocDoclintAll --console=plain --warning-mode=all --no-daemon > /tmp/warnings-jda.txt 2>&1) || true
/tmp/"$USER"/plume-scripts/ci-lint-diff /tmp/warnings-jda.txt || status=1
if [ $status -ne 0 ]; then exit $status; fi

# User documentation
make -C docs/manual all
