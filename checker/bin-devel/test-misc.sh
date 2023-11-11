#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
# shellcheck disable=SC1090 # In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
export ORG_GRADLE_PROJECT_useJdk17Compiler=true
source "$SCRIPTDIR"/clone-related.sh

PLUME_SCRIPTS="$SCRIPTDIR/.plume-scripts"

status=0

## Code style and formatting
JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//')
if [ "${JAVA_VER}" != "8" ] && [ "${JAVA_VER}" != "20" ] ; then
  ./gradlew spotlessCheck --console=plain --warning-mode=all
fi
if grep -n -r --exclude-dir=build --exclude-dir=examples --exclude-dir=jtreg --exclude-dir=tests --exclude="*.astub" --exclude="*.tex" '^\(import static \|import .*\*;$\)'; then
  echo "Don't use static import or wildcard import"
  exit 1
fi
make -C checker/bin
make -C checker/bin-devel
make -C docs/developer/release check-python-style

## HTML legality
./gradlew htmlValidate --console=plain --warning-mode=all

## Javadoc documentation
# Try twice in case of network lossage.
(./gradlew javadoc --console=plain --warning-mode=all || (sleep 60 && ./gradlew javadoc --console=plain --warning-mode=all)) || status=1
./gradlew javadocPrivate --console=plain --warning-mode=all || status=1
# For refactorings that touch a lot of code that you don't understand, create
# top-level file SKIP-REQUIRE-JAVADOC.  Delete it after the pull request is merged.
if [ -f SKIP-REQUIRE-JAVADOC ]; then
  echo "Skipping requireJavadoc because file SKIP-REQUIRE-JAVADOC exists."
else
  (./gradlew requireJavadoc --console=plain --warning-mode=all > /tmp/warnings-rjp.txt 2>&1) || true
  "$PLUME_SCRIPTS"/ci-lint-diff /tmp/warnings-rjp.txt || status=1
  (./gradlew javadocDoclintAll --console=plain --warning-mode=all > /tmp/warnings-jda.txt 2>&1) || true
  "$PLUME_SCRIPTS"/ci-lint-diff /tmp/warnings-jda.txt || status=1
fi
if [ $status -ne 0 ]; then exit $status; fi


## User documentation
./gradlew manual
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
