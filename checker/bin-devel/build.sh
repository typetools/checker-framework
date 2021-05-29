#!/bin/bash

echo Entering checker/bin-devel/build.sh in "$(pwd)"

# Fail the whole script if any command fails
set -e

echo "initial CHECKERFRAMEWORK=$CHECKERFRAMEWORK"
export CHECKERFRAMEWORK="${CHECKERFRAMEWORK:-$(pwd -P)}"
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

if [ "$(uname)" == "Darwin" ] ; then
  export JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home)}
else
  # shellcheck disable=SC2230
  export JAVA_HOME=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(which javac)")")")}
fi
echo "JAVA_HOME=${JAVA_HOME}"

# Using `(cd "$CHECKERFRAMEWORK" && ./gradlew getPlumeScripts -q)` leads to infinite regress.
PLUME_SCRIPTS="$CHECKERFRAMEWORK/checker/bin-devel/.plume-scripts"
if [ -d "$PLUME_SCRIPTS" ] ; then
  (cd "$PLUME_SCRIPTS" && git pull -q)
else
  (cd "$CHECKERFRAMEWORK/checker/bin-devel" && \
      (git clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git .plume-scripts || \
       git clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git .plume-scripts))
fi

# Clone the annotated JDK into ../jdk .
"$PLUME_SCRIPTS/git-clone-related" typetools jdk

AFU="${AFU:-../annotation-tools/annotation-file-utilities}"
# Don't use `AT=${AFU}/..` which causes a git failure.
AT=$(dirname "${AFU}")

## Build annotation-tools (Annotation File Utilities)
"$PLUME_SCRIPTS/git-clone-related" typetools annotation-tools "${AT}"
if [ ! -d ../annotation-tools ] ; then
  ln -s "${AT}" ../annotation-tools
fi

echo "Running:  (cd ${AT} && ./.travis-build-without-test.sh)"
(cd "${AT}" && ./.travis-build-without-test.sh)
echo "... done: (cd ${AT} && ./.travis-build-without-test.sh)"


## Build stubparser
"$PLUME_SCRIPTS/git-clone-related" typetools stubparser
echo "Running:  (cd ../stubparser/ && ./.travis-build-without-test.sh)"
(cd ../stubparser/ && ./.travis-build-without-test.sh)
echo "... done: (cd ../stubparser/ && ./.travis-build-without-test.sh)"


## Build JSpecify, only for the purpose of using its tests.
"$PLUME_SCRIPTS/git-clone-related" jspecify jspecify
if type -p java; then
  _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
  _java="$JAVA_HOME/bin/java"
else
  echo "Can't find java"
  exit 1
fi
version=$("$_java" -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
if [[ "$version" -ge 9 ]]; then
  echo "Running:  (cd ../jspecify/ && ./gradlew build)"
  ## Try twice in case of network lossage.
  (cd ../jspecify/ && ./gradlew build) || (sleep 60 && cd ../jspecify/ && ./gradlew build)
  echo "... done: (cd ../jspecify/ && ./gradlew build)"
fi


## Compile

# Downloading the gradle wrapper sometimes fails.
# If so, the next command gets another chance to try the download.
(./gradlew help || sleep 10) > /dev/null 2>&1

echo "running \"./gradlew assemble\" for checker-framework"
./gradlew assemble --console=plain --warning-mode=all -s --no-daemon -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

echo Exiting checker/bin-devel/build.sh in "$(pwd)"
