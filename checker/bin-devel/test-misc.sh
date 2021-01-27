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

PLUME_SCRIPTS="$SCRIPTDIR/.plume-scripts"

# Checker Framework demos
"$PLUME_SCRIPTS/git-clone-related" typetools checker-framework.demos
./gradlew :checker:demosTests --console=plain --warning-mode=all --no-daemon

status=0

# Code style and formatting
./gradlew checkBasicStyle checkFormat --console=plain --warning-mode=all --no-daemon
if grep -n -r --exclude-dir=build --exclude-dir=examples --exclude-dir=jtreg --exclude-dir=tests --exclude="*.astub" --exclude="*.tex" '^\(import static \|import .*\*;$\)'; then
  echo "Don't use static import or wildcard import"
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
  "$PLUME_SCRIPTS"/ci-lint-diff /tmp/warnings-rjp.txt || status=1
  (./gradlew javadocDoclintAll --console=plain --warning-mode=all --no-daemon > /tmp/warnings-jda.txt 2>&1) || true
  "$PLUME_SCRIPTS"/ci-lint-diff /tmp/warnings-jda.txt || status=1
fi
if [ $status -ne 0 ]; then exit $status; fi


# User documentation
make -C docs/manual all
git diff  --exit-code docs/manual/contributors.tex || \
    (set +x && set +v &&
     echo "docs/manual/contributors.tex is not up to date." &&
     echo "If the above suggestion is appropriate, run: make -C docs/manual contributors.tex" &&
     echo "If the suggestion contains a username rather than a human name, then do all the following:" &&
     echo "  * Update your git configuration by running:  git config --global user.name \"YOURFULLNAME\"" &&
     echo "  * Add your name to your GitHub account profile at https://github.com/settings/profile" &&
     echo "  * Make a pull request to add your GitHub ID to" &&
     echo "    https://github.com/plume-lib/plume-scripts/blob/master/git-authors.sed" &&
     echo "    and remake contributors.tex after that pull request is merged." &&
     false)
