#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS

if [ -d "/tmp/plume-scripts" ] ; then
  (cd /tmp/plume-scripts && git pull -q)
else
  (cd /tmp && git clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git)
fi

export CHECKERFRAMEWORK="${CHECKERFRAMEWORK:-$(pwd -P)}"
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source $SCRIPTDIR/build.sh ${BUILDJDK}


## checker-framework-inference is a downstream test, but run it in its
## own group because it is most likely to fail, and it's helpful to see
## that only it, not other downstream tests, failed.

/tmp/plume-scripts/git-clone-related opprop checker-framework-inference

export AFU="${AFU:-$(cd ../annotation-tools/annotation-file-utilities && pwd -P)}"
export PATH=$AFU/scripts:$PATH
(cd ../checker-framework-inference && ./.travis-build.sh)
