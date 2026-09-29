#!/bin/bash

# Runs the Nullness Checker over Apache Beam (https://github.com/typetools/beam), using this
# Checker Framework.  Only crashes fail the build:  the Beam fork suppresses all warnings.
# The optional argument is a group of Beam modules:  "part1", "part2", or "all" (the default).
# The groups are defined by typecheck.sh in the Beam fork.

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

GROUPARG=${1:-all}
echo "GROUPARG=$GROUPARG"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

source "$SCRIPT_DIR"/clone-related.sh

# Beam's Gradle version cannot run on a JDK newer than 21.
if ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "21[."]'; then
  echo "$0: JAVA_HOME=$JAVA_HOME is not JDK 21; set JAVA21_HOME to a JDK 21." >&2
  exit 1
fi

gradle_retry assembleForJavac -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

"$SCRIPT_DIR/.git-scripts/git-clone-related" typetools beam
cd ../beam

# typecheck.sh retries Gradle runs that fail for network reasons.
./typecheck.sh "$GROUPARG"
