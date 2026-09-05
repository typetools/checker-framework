#!/bin/bash

echo "Entering checker/bin-devel/clone-related.sh $* in $(pwd)"

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

# Runs "./gradlew" with the arguments after the first, in the current
# directory.  Retries if the failure looks like a transient network problem,
# such as HTTP status code 429 ("Too Many Requests") from Maven Central.  The
# pattern below matches only messages that indicate a network problem; in
# particular it does not match Gradle's "Could not resolve".
# The first argument is a space-separated list of the delays, in seconds,
# before successive retries.  Its last element must be 0, which means "do not
# retry again".
gradle_retry_with_delays() {
  local log status delay
  local -a delays
  read -r -a delays <<< "$1"
  shift
  log="$(mktemp)"
  for delay in "${delays[@]}"; do
    # "set +e" keeps a failure of the pipeline from aborting the script before
    # the retry logic below runs; it is needed whether or not "set -o pipefail"
    # is in effect.  ${PIPESTATUS[0]} is the exit status of "./gradlew".
    set +e
    ./gradlew "$@" 2>&1 | tee "$log"
    status="${PIPESTATUS[0]}"
    set -e
    if [ "$status" -eq 0 ]; then
      rm -f "$log"
      return 0
    fi
    if [ "$delay" -eq 0 ] \
      || ! grep -q -E '(status|response) code:? (429|5[0-9][0-9])|Connect(ion)? timed out|Connection (reset|refused)|Read timed out|Network is unreachable|UnknownHostException|Temporary failure in name resolution|Premature end of Content-Length|Remote host terminated the handshake' "$log"; then
      rm -f "$log"
      return "$status"
    fi
    echo "gradle_retry: \"./gradlew $*\" failed for an apparent network reason; retrying in ${delay} seconds."
    sleep "$delay"
  done
}

# Runs "./gradlew" with the given arguments, in the current directory, retrying
# up to twice if the failure looks like a transient network problem.
gradle_retry() {
  gradle_retry_with_delays "60 300 0" "$@"
}

# Like "gradle_retry", but retries at most once and after a shorter delay.  Use
# this for a task that runs for a long time, where a full sequence of retries
# could exceed the CI job's time limit and thereby hide the underlying failure.
gradle_retry_once() {
  gradle_retry_with_delays "60 0" "$@"
}

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

# Download Gradle and dependencies, retrying in case of network problems,
# before the expensive part of the build.
# Do not do this under CircleCI:  every CircleCI job sources this script, so on
# a cache miss all of them would resolve every configuration against Maven
# Central at once, which is what provokes HTTP status code 429.
# echo "NO_WRITE_VERIFICATION_METADATA=$NO_WRITE_VERIFICATION_METADATA"
if [ -z "${CIRCLECI:-}" ] && [ -z "${NO_WRITE_VERIFICATION_METADATA+x}" ]; then
  (TERM=dumb gradle_retry --write-verification-metadata sha256 help --dry-run --quiet)
fi

echo "Exiting checker/bin-devel/clone-related.sh $* in $(pwd)"
