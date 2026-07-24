#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

source "$SCRIPT_DIR"/clone-related.sh

./gradlew assembleForJavac -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

# TODO: Maybe I should move this into the CI job, and do it for all CI jobs.
cp "$SCRIPT_DIR"/mvn-settings.xml ~/settings.xml

"$SCRIPT_DIR/.git-scripts/git-clone-related" typetools guava
cd ../guava

if [ "$TRAVIS" = "true" ]; then
  # There are two reasons that this script does not work on Travis.
  # 1. Travis kills jobs that do not produce output for 10 minutes.  (This can be worked around.)
  # 2. Travis kills jobs that produce too much output.  (This cannot be worked around.)
  echo "On Travis, use scripts that run just one type-checker."
  exit 1
fi

## This command works locally, but on Azure it fails with timeouts while downloading Maven dependencies.
# cd guava && time mvn --debug -B package -P checkerframework-local -Dmaven.test.skip=true -Danimal.sniffer.skip=true

# Pre-download Maven dependencies.  Otherwise there are sometimes timeouts when downloading a Maven dependency.
(cd guava \
  && (timeout 5m mvn -B dependency:resolve-plugins || (sleep 1m && (timeout 5m mvn -B dependency:resolve-plugins || true))))
# This downloads even more dependencies, but the above seems to be sufficient.
# (cd guava && \
# (timeout 5m mvn -B dependency:go-offline || (sleep 1m && (timeout 5m mvn -B dependency:go-offline || true))))

# Comment about -D flags: the maven.wagon settings should not be relevant to Maven 3.9 and later, but try them anyway.
# (I saw Maven take 30 minutes to download a dependency even with all these flags.)
# (cd guava \
#   && time mvn --debug -B compile -P checkerframework-local \
#     -Dhttp.keepAlive=false -Daether.connector.http.connectionMaxTtl=25 -Dmaven.wagon.http.pool=false -Dmaven.wagon.httpconnectionManager.ttlSeconds=120)

echo "Starting FormatterChecker"
(cd guava \
  && mvn -B clean \
  && mvn -B compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.formatter.FormatterChecker)
echo "Finished FormatterChecker"
echo "Starting IndexChecker"
(cd guava \
  && mvn -B clean \
  && mvn -B compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.index.IndexChecker)
echo "Finished IndexChecker"
echo "Starting InterningChecker"
(cd guava \
  && mvn -B clean \
  && mvn -B compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.interning.InterningChecker)
echo "Finished InterningChecker"
echo "Starting LockChecker"
(cd guava \
  && mvn -B clean \
  && mvn -B compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.lock.LockChecker)
echo "Finished LockChecker"
echo "Starting NullnessChecker"
(cd guava \
  && mvn -B clean \
  && mvn -B compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.nullness.NullnessChecker)
echo "Finished NullnessChecker"
echo "Starting RegexChecker"
(cd guava \
  && mvn -B clean \
  && mvn -B compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.regex.RegexChecker)
echo "Finished RegexChecker"
echo "Starting ResourceLeakChecker"
(cd guava \
  && mvn -B clean \
  && mvn -B compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.resourceleak.ResourceLeakChecker)
echo "Finished ResourceLeakChecker"
echo "Starting SignatureChecker"
(cd guava \
  && mvn -B clean \
  && mvn -B compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.signature.SignatureChecker)
echo "Finished SignatureChecker"
echo "Starting SignednessChecker"
(cd guava \
  && mvn -B clean \
  && mvn -B compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.signedness.SignednessChecker)
echo "Finished SignednessChecker"
