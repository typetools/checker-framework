#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
export ORG_GRADLE_PROJECT_useJdk21Compiler=true
source "$SCRIPT_DIR"/clone-related.sh

"$SCRIPT_DIR/.git-scripts/git-clone-related" typetools guava
cd ../guava

./typecheck.sh signature
