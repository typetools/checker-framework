#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
export ORG_GRADLE_PROJECT_useJdk17Compiler=true
source "$SCRIPTDIR"/clone-related.sh



./gradlew test --console=plain --warning-mode=all
