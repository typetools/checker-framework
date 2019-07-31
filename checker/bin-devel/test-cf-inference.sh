#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS

git -C /tmp/plume-scripts pull > /dev/null 2>&1 \
  || git -C /tmp clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git
eval=`/tmp/plume-scripts/ci-info typetools`

export CHECKERFRAMEWORK=`readlink -f ${CHECKERFRAMEWORK:-.}`
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source $SCRIPTDIR/build.sh ${BUILDJDK}


## checker-framework-inference is a downstream test, but run it in its
## own group because it is most likely to fail, and it's helpful to see
## that only it, not other downstream tests, failed.

REPO=`/tmp/plume-scripts/git-find-fork ${CI_ORGANIZATION} typetools checker-framework-inference`
BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${CI_BRANCH}`
(cd .. && git clone -b ${BRANCH} -q --single-branch --depth 1 ${REPO}) || (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO})

export AFU=`readlink -f ${AFU:-../annotation-tools/annotation-file-utilities}`
export PATH=$AFU/scripts:$PATH
(cd ../checker-framework-inference && ./gradlew dist test --console=plain --warning-mode=all --no-daemon)
