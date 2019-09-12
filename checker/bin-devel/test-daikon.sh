#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS

git -C /tmp/plume-scripts pull > /dev/null 2>&1 \
  || git -C /tmp clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git

export CHECKERFRAMEWORK="${CHECKERFRAMEWORK:-$(pwd -P)}"
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source $SCRIPTDIR/build.sh ${BUILDJDK}


# daikon-typecheck: 15 minutes
/tmp/plume-scripts/git-clone-related codespecs daikon

cd ../daikon
make compile
make -C java typecheck
