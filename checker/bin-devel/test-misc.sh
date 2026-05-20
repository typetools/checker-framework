#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
# This script tests that the CF builds using the installed JDK, so don't add the following:
# export ORG_GRADLE_PROJECT_useJdk21Compiler=true
# That means that this script cannot be run under Java 17.

source "$SCRIPT_DIR"/clone-related.sh

PLUME_SCRIPTS="$SCRIPT_DIR/.plume-scripts"

## Code style and formatting
JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//')
if [ "${JAVA_VER}" != "8" ] && [ "${JAVA_VER}" != "11" ]; then
  # # spotlessGroovy sometimes hangs and sometimes fails with:
  # # "java.io.IOException: Failed to provision P2 dependencies".
  # echo "Starting: ./gradlew spotlessGroovy"
  # ./gradlew spotlessGroovy > /dev/null 2>&1 || (echo "spotlessGroovy failed" && sleep 60 && true)
  # echo "Finished: ./gradlew spotlessGroovy"
  ./gradlew spotlessCheck --warning-mode=all
fi
if grep -n -r --exclude-dir=build --exclude-dir=examples --exclude-dir=jtreg --exclude-dir=tests --exclude="*.astub" --exclude="*.tex" '^\(import static \|import .*\*;$\)'; then
  echo "Don't use static import or wildcard import"
  exit 1
fi

# Under CI, there are two CPUs, but limit to 1 to avoid out-of-memory error.
if [ -n "$("$CHECKERFRAMEWORK"/checker/bin-devel/is-ci.sh)" ]; then
  num_jobs=1
else
  num_jobs="$(nproc || sysctl -n hw.ncpu || getconf _NPROCESSORS_ONLN || echo 1)"
fi
make style-check --jobs="${num_jobs}"

declare -a failures=()

## Javadoc documentation
# Try twice in case of network lossage.
(./gradlew javadoc --warning-mode=all || (sleep 60 && ./gradlew javadoc --warning-mode=all)) || failures+=("gradlew javadoc")
./gradlew javadocPrivate --warning-mode=all || failures+=("gradlew javadocPrivate")
./gradlew buildSrc:javadoc --warning-mode=all || failures+=("gradlew buildSrc:javadoc")
# For refactorings that touch a lot of code that you don't understand, create
# top-level file SKIP-REQUIRE-JAVADOC.  Delete it after the pull request is merged.
if [ -f SKIP-REQUIRE-JAVADOC ]; then
  echo "Skipping requireJavadoc because file SKIP-REQUIRE-JAVADOC exists."
else
  (./gradlew requireJavadoc --warning-mode=all > /tmp/warnings-requireJavadoc.txt 2>&1) || true
  "$PLUME_SCRIPTS"/ci-lint-diff /tmp/warnings-requireJavadoc.txt || failures+=("ci-lint-diff /tmp/warnings-requireJavadoc.txt")
  (./gradlew javadocDoclintAll --warning-mode=all > /tmp/warnings-javadocDoclintAll.txt 2>&1) || true
  "$PLUME_SCRIPTS"/ci-lint-diff /tmp/warnings-javadocDoclintAll.txt || failures+=("ci-lint-diff /tmp/warnings-javadocDoclintAll.txt")
fi
if [ ${#failures[@]} -gt 0 ]; then
  echo "Failures:"
  printf '%s\n' "${failures[@]}"
  exit 1
fi

## User documentation
./gradlew manual
git diff --exit-code docs/manual/contributors.tex \
  || (set +x && set +v \
    && echo "docs/manual/contributors.tex is not up to date." \
    && echo "If the above suggestion is appropriate, run: make -C docs/manual contributors.tex" \
    && echo "If the suggestion contains a username rather than a human name, then do all the following:" \
    && echo "* Update your git configuration by running:  git config --global user.name \"YOURFULLNAME\"" \
    && echo "* Add your name to your GitHub account profile at https://github.com/settings/profile" \
    && echo "* Make a pull request to add your GitHub ID to" \
    && echo "  https://github.com/plume-lib/git-scripts/blob/master/git-authors.sed" \
    && echo "* After that pull request is merged, run: make -C docs/manual contributors.tex" \
    && false)

## Listing tasks should succeed; this helps ensure importing Checker Framework into IDEs like IntelliJ works.
./gradlew tasks --all --warning-mode=all
