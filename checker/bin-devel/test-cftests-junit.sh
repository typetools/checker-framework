#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

source "$SCRIPT_DIR"/clone-related.sh

# `./gradlew test` subsumes the other commands, but performing them
# separately seems to avoid some out-of-memory errors.
./gradlew assemble --warning-mode=all
./gradlew compileTestJava testClasses --warning-mode=all

if [ "$#" -eq 0 ]; then
  arg=both
elif [ "$#" -eq 1 ]; then
  arg=$1
else
  echo "$0 expects 0 or 1 arguments:" "$@"
  exit 2
fi

## Split "test" into its parts (up to date as of 2025-11-02).
## As of 2025-11-02, :checker:test took 11.5m and everything except
## :checker:test took 15m.  (:framework:test took 6m.)
# ./gradlew test --warning-mode=all

if [ "$arg" != "part2" ]; then
  ./gradlew junitPart1 --warning-mode=all
fi

if [ "$arg" != "part1" ]; then
  ./gradlew junitPart2 --warning-mode=all
fi
