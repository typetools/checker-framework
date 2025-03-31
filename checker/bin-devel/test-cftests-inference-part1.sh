#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
export ORG_GRADLE_PROJECT_useJdk21Compiler=true
source "$SCRIPTDIR"/clone-related.sh



./gradlew inferenceTests-part1 --console=plain --warning-mode=all
