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


# Checker Framework demos
"/tmp/$USER/plume-scripts/git-clone-related" typetools checker-framework.demos
./gradlew :checker:demosTests --console=plain --warning-mode=all --no-daemon

status=0

# Code style and formatting
./gradlew checkBasicStyle checkFormat --console=plain --warning-mode=all --no-daemon
if grep -n -r --exclude-dir=build --exclude-dir=jtreg --exclude-dir=tests "^import static "; then
  echo "Don't use static import"
  exit 1
fi

# HTML legality
./gradlew htmlValidate --console=plain --warning-mode=all --no-daemon

# Javadoc documentation
./gradlew javadoc --console=plain --warning-mode=all --no-daemon || status=1
./gradlew javadocPrivate --console=plain --warning-mode=all --no-daemon || status=1
# For refactorings that touch a lot of code that you don't understand, create
# top-level file SKIP-REQUIRE-JAVADOC.  Delete it after the pull request is merged.
if [ ! -f SKIP-REQUIRE-JAVADOC ]; then
  (./gradlew requireJavadoc --console=plain --warning-mode=all --no-daemon > /tmp/warnings-rjp.txt 2>&1) || true
  /tmp/"$USER"/plume-scripts/ci-lint-diff /tmp/warnings-rjp.txt || status=1
  (./gradlew javadocDoclintAll --console=plain --warning-mode=all --no-daemon > /tmp/warnings-jda.txt 2>&1) || true
  /tmp/"$USER"/plume-scripts/ci-lint-diff /tmp/warnings-jda.txt || status=1
fi
if [ $status -ne 0 ]; then exit $status; fi


# User documentation
make -C docs/manual all
