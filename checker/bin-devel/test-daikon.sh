#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS

if [ -d "/tmp/plume-scripts" ] ; then
  (cd /tmp/plume-scripts && git pull -q) > /dev/null 2>&1
else
  (cd /tmp && git clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git)
fi

export CHECKERFRAMEWORK="${CHECKERFRAMEWORK:-$(pwd -P)}"
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source $SCRIPTDIR/build.sh ${BUILDJDK}


# daikon-typecheck: 15 minutes
/tmp/plume-scripts/git-clone-related codespecs daikon

cd ../daikon
make compile
time make -C java typecheck
