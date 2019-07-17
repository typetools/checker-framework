#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS

git -C /tmp/plume-scripts pull > /dev/null 2>&1 \
  || git -C /tmp clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git
eval `/tmp/plume-scripts/ci-info typetools`

export CHECKERFRAMEWORK=`readlink -f ${CHECKERFRAMEWORK:-.}`
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source $SCRIPTDIR/build.sh ${BUILDJDK}


# plume-lib-typecheck: 15 minutes
REPO=`/tmp/plume-scripts/git-find-fork ${CI_ORGANIZATION} typetests plume-lib-typecheck`
BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${CI_BRANCH}`
(cd .. && git clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO}) || (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO})

(cd ../plume-lib-typecheck && ./.travis-build.sh)
