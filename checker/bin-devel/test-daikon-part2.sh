#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
export ORG_GRADLE_PROJECT_useJdk21Compiler=true
source "$SCRIPTDIR"/clone-related.sh

# Run assembleForJavac because it does not build the javadoc, so it is faster than assemble.
echo "running \"./gradlew assembleForJavac\" for checker-framework"
./gradlew assembleForJavac --console=plain -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

# daikon-typecheck: 15 minutes
"$SCRIPTDIR/.git-scripts/git-clone-related" codespecs daikon
cd ../daikon
git log | head -n 5
make --jobs="$(getconf _NPROCESSORS_ONLN)" compile
if [ "$TRAVIS" = "true" ] ; then
  # Travis kills a job if it runs 10 minutes without output
  time make JAVACHECK_EXTRA_ARGS=-Afilenames -C java --jobs="$(getconf _NPROCESSORS_ONLN)" typecheck-part2
else
  time make -C java --jobs="$(getconf _NPROCESSORS_ONLN)" typecheck-part2
fi
