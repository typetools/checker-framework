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

(cd "${AFU}" && \
  TERM=dumb timeout 300 ./gradlew --write-verification-metadata sha256 help --dry-run </dev/null >/dev/null 2>&1 || \
  TERM=dumb ./gradlew --write-verification-metadata sha256 help --dry-run </dev/null >/dev/null 2>&1 || \
  (sleep 60s && TERM=dumb ./gradlew --write-verification-metadata sha256 help --dry-run))

(cd "${AFU}" && ./gradlew assemble)

if [ -d "/tmp/$USER/git-scripts" ] ; then
  (cd "/tmp/$USER/git-scripts" && (git pull -q || true)) > /dev/null 2>&1
else
  mkdir -p "/tmp/$USER" && git -C "/tmp/$USER" clone --depth=1 -q https://github.com/plume-lib/git-scripts.git
fi

# checker-framework and its downstream tests
"/tmp/$USER/git-scripts/git-clone-related" typetools checker-framework "${CHECKERFRAMEWORK}"

(cd "${CHECKERFRAMEWORK}/checker" && ../gradlew ainferTest)
