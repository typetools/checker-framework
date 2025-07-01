#!/bin/sh

set -e
set -o verbose
set -o xtrace
export SHELLOPTS

if [ "$(uname)" = "Darwin" ] ; then
  export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home)}"
else
  export JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(which javac)")")")}"
fi
export AFU="${AFU:-$(cd annotation-file-utilities >/dev/null 2>&1 && pwd -P)}"
export CHECKERFRAMEWORK="${CHECKERFRAMEWORK:-$(cd .. >/dev/null 2>&1 && pwd -P)/checker-framework}"
export PATH="$AFU/scripts:$JAVA_HOME/bin:$PATH"

if [ -d "/tmp/$USER/plume-scripts" ] ; then
  (cd "/tmp/$USER/plume-scripts" && (git pull -q || true)) > /dev/null 2>&1
else
  mkdir -p "/tmp/$USER" && git -C "/tmp/$USER" clone --depth=1 -q https://github.com/plume-lib/plume-scripts.git
fi
PLUME_SCRIPTS="/tmp/$USER/plume-scripts"

(cd "${AFU}" && \
  TERM=dumb timeout 300 ./gradlew --write-verification-metadata sha256 help --dry-run </dev/null >/dev/null 2>&1 || \
  TERM=dumb ./gradlew --write-verification-metadata sha256 help --dry-run </dev/null >/dev/null 2>&1 || \
  (sleep 60s && TERM=dumb ./gradlew --write-verification-metadata sha256 help --dry-run))

cd "${AFU}"
./gradlew assemble

status=0

## Code style and formatting
JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//')
if [ "${JAVA_VER}" != "8" ] && [ "${JAVA_VER}" != "20" ] ; then
  ./gradlew spotlessCheck --console=plain --warning-mode=all --no-daemon || status=1
fi

# HTML legality
./gradlew htmlValidate --console=plain --warning-mode=all --no-daemon || status=1

# Javadoc documentation
# For refactorings that touch a lot of code that you don't understand, create
# top-level file SKIP-REQUIRE-JAVADOC.  Delete it after the pull request is merged.
if [ -f ../SKIP-REQUIRE-JAVADOC ]; then
  echo "Skipping Javadoc tasks because file SKIP-REQUIRE-JAVADOC exists."
else
  (./gradlew requireJavadoc --console=plain --warning-mode=all --no-daemon > /tmp/warnings-rjp.txt 2>&1) || true
  "$PLUME_SCRIPTS"/ci-lint-diff /tmp/warnings-rjp.txt || status=1
  (./gradlew javadoc --console=plain --warning-mode=all --no-daemon > /tmp/warnings-javadoc.txt 2>&1) || true
  "$PLUME_SCRIPTS"/ci-lint-diff /tmp/warnings-javadoc.txt || status=1
  (./gradlew javadocPrivate --console=plain --warning-mode=all --no-daemon > /tmp/warnings-javadocPrivate.txt 2>&1) || true
  "$PLUME_SCRIPTS"/ci-lint-diff /tmp/warnings-javadocPrivate.txt || status=1
fi
if [ $status -ne 0 ]; then exit $status; fi
