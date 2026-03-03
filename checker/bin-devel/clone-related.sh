#!/bin/bash

echo Entering checker/bin-devel/clone-related.sh in "$(pwd)"

# Fail the whole script if any command fails
set -e

DEBUG=0
# To enable debugging, uncomment the following line.
# DEBUG=1

if [ $DEBUG -eq 0 ]; then
  DEBUG_FLAG=
else
  DEBUG_FLAG=--debug
fi

export CHECKERFRAMEWORK="${CHECKERFRAMEWORK:-$(pwd -P)}"
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

IS_CI="$("$SCRIPT_DIR"/is-ci.sh)"
export IS_CI
if [ -n "$IS_CI" ]; then
  # CircleCI fails, for the Daikon job only, if "-Dorg.gradle.daemon=false" is removed.
  export GRADLE_OPTS="${GRADLE_OPTS} -Dorg.gradle.daemon=false -Dorg.gradle.console=plain -Xmx4g"
fi

export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

echo "initial JAVA_HOME=${JAVA_HOME}"
if [ "$(uname)" == "Darwin" ]; then
  export JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home)}
else
  # shellcheck disable=SC2230
  export JAVA_HOME=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(which javac)")")")}
fi
echo "JAVA_HOME=${JAVA_HOME}"

# Using `(cd "$CHECKERFRAMEWORK" && ./gradlew getGitScripts -q)` leads to infinite regress.
GIT_SCRIPTS="${SCRIPT_DIR}/.git-scripts"
if [ -d "$GIT_SCRIPTS" ]; then
  (cd "$GIT_SCRIPTS" && (git pull -q || true))
else
  (cd "${SCRIPT_DIR}" \
    && (git clone --depth=1 -q https://github.com/plume-lib/git-scripts.git .git-scripts \
      || (sleep 60 && git clone --depth=1 -q https://github.com/plume-lib/git-scripts.git .git-scripts)))
fi

# Clone the annotated JDK into ../jdk .
"$GIT_SCRIPTS/git-clone-related" ${DEBUG_FLAG} typetools jdk

### Commented temporarily because JSpecify build is failing under JDK 17.
### (I guess they don't use continuous integration.)
# ## Build JSpecify, only for the purpose of using its tests.
# "$GIT_SCRIPTS/git-clone-related" jspecify jspecify
# if type -p java; then
#   _java=java
# elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
#   _java="$JAVA_HOME/bin/java"
# else
#   echo "Can't find java"
#   exit 1
# fi
# version=$("$_java" -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//')
# if [[ "$version" -ge 9 ]]; then
#   echo "Running:  (cd ../jspecify/ && ./gradlew build)"
#   # If failure, retry in case the failure was due to network lossage.
#   (cd ../jspecify/ && export JDK_JAVA_OPTIONS='--add-opens jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED' && (./gradlew build || (sleep 60s && ./gradlew build)))
#   echo "... done: (cd ../jspecify/ && ./gradlew build)"
# fi

# Test that the CF, when built with JDK 21, works on other JDKs.
export ORG_GRADLE_PROJECT_useJdk21Compiler=true

# Set JAVA_HOME to JDK 21 so that Gradle runs using Java 21.
# Prefer an OS-appropriate default only if JAVA21_HOME is unset and exists.
if [ -z "${JAVA21_HOME:-}" ]; then
  if [ "$(uname)" = "Darwin" ]; then
    CANDIDATE="$(/usr/libexec/java_home -v 21 2> /dev/null || true)"
    [ -n "$CANDIDATE" ] && export JAVA21_HOME="$CANDIDATE"
  elif [ -d /usr/lib/jvm/java-21-openjdk-amd64 ]; then
    export JAVA21_HOME=/usr/lib/jvm/java-21-openjdk-amd64
  fi
fi
# Only override JAVA_HOME if JAVA21_HOME points to a usable JDK.
if [ -n "${JAVA21_HOME:-}" ] && [ -x "${JAVA21_HOME}/bin/java" ]; then
  export JAVA_HOME="${JAVA21_HOME}"
fi

# Download Gradle and dependencies, retrying in case of network problems.
# Under CircleCI, the `timeout` command seems to hang forever.
if [ -z "$CIRCLECI" ]; then
  # echo "NO_WRITE_VERIFICATION_METADATA=$NO_WRITE_VERIFICATION_METADATA"
  if [ -z "${NO_WRITE_VERIFICATION_METADATA+x}" ]; then
    # Note that "timeout" is not compatible with shell functions.
    TERM=dumb ./gradlew --write-verification-metadata sha256 help --dry-run --quiet \
      || { echo "./gradlew --write-verification-metadata sha256 help --dry-run failed; sleeping before trying again." \
        && sleep 1m \
        && echo "Trying again: ./gradlew --write-verification-metadata sha256 help --dry-run" \
        && TERM=dumb ./gradlew --write-verification-metadata sha256 help --dry-run; }
  fi
fi

echo Exiting checker/bin-devel/clone-related.sh in "$(pwd)"
