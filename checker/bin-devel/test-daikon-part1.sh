#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

source "$SCRIPT_DIR"/clone-related.sh

# Run assembleForJavac because it does not build the javadoc, so it is faster than assemble.
echo "running \"./gradlew assembleForJavac\" for checker-framework"
./gradlew assembleForJavac -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000

"$SCRIPT_DIR/.git-scripts/git-clone-related" codespecs daikon
cd ../daikon
git log | head -n 5

# Under CI, there are two CPUs, but limit to 1 to avoid out-of-memory error.
if [ -n "$("$CHECKERFRAMEWORK"/checker/bin-devel/is-ci.sh)" ]; then
  num_jobs=1
else
  num_jobs="$(nproc || sysctl -n hw.ncpu || getconf _NPROCESSORS_ONLN || echo 1)"
fi

make --jobs="${num_jobs}" compile
if [ "$TRAVIS" = "true" ]; then
  # Travis kills a job if it runs 10 minutes without output
  time make JAVACHECK_EXTRA_ARGS=-Afilenames -C java --jobs="${num_jobs}" typecheck-part1
else
  time make -C java --jobs="${num_jobs}" typecheck-part1
fi
