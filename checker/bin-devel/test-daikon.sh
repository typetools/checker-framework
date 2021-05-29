#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
# In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
# shellcheck disable=SC1090
source "$SCRIPTDIR"/build.sh


# daikon-typecheck: 15 minutes
"$SCRIPTDIR/.plume-scripts/git-clone-related" codespecs daikon
cd ../daikon
git log | head -n 5
make compile
if [ "$TRAVIS" = "true" ] ; then
  # Travis kills a job if it runs 10 minutes without output
  time make JAVACHECK_EXTRA_ARGS=-Afilenames -C java typecheck
else
  time make -C java typecheck
fi
