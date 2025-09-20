#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
# Test that the CF, when built with JDK 21, works on other JDKs.
export ORG_GRADLE_PROJECT_useJdk21Compiler=true

# Run Gradle using Java 21.
mkdir ~/.gradle && echo "org.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64" >> ~/.gradle/gradle.properties

source "$SCRIPT_DIR"/clone-related.sh

gradle_ci getPlumeScripts
PLUME_SCRIPTS="$SCRIPT_DIR/.plume-scripts"

# Pluggable type-checking:  run the Checker Framework on itself
gradle_ci typecheck-part2 --warning-mode=all

if [ -f SKIP-REQUIRE-JAVADOC ]; then
  echo "Skipping checkNullness because file SKIP-REQUIRE-JAVADOC exists."
else
  (gradle_ci checkNullness -PnullnessAll --warning-mode=all > /tmp/warnings-checkNullness.txt 2>&1) || true
  "$PLUME_SCRIPTS"/ci-lint-diff /tmp/warnings-checkNullness.txt
fi
