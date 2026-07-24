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

# Here are times for GitHub Actions on 2026-06-23.  Azure Pipelines and CircleCI are slower.
# Finished FormatterChecker in 197 seconds
# Finished IndexChecker in 1024 seconds
# Finished InterningChecker in 192 seconds
# Finished LockChecker in 332 seconds
# Finished NullnessChecker in 403 seconds
# Finished RegexChecker in 193 seconds
# Finished ResourceLeakChecker in 365 seconds
# Finished SignatureChecker in 193 seconds
# Finished SignednessChecker in 327 seconds

echo "Starting FormatterChecker"
start_time=$(date +%s)
(cd guava \
  && mvn -B clean \
  && mvn -B compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.formatter.FormatterChecker)
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo "Finished FormatterChecker in $elapsed seconds"

echo "Starting IndexChecker"
start_time=$(date +%s)
(cd guava \
  && mvn -B clean \
  && mvn -B compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.index.IndexChecker)
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo "Finished IndexChecker in $elapsed seconds"

echo "Starting InterningChecker"
start_time=$(date +%s)
(cd guava \
  && mvn -B clean \
  && mvn -B compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.interning.InterningChecker)
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo "Finished InterningChecker in $elapsed seconds"

echo "Starting LockChecker"
start_time=$(date +%s)
(cd guava \
  && mvn -B clean \
  && mvn -B compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.lock.LockChecker)
end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo "Finished LockChecker in $elapsed seconds"
