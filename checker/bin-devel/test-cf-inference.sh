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


## checker-framework-inference is a downstream test, but run it in its own
## script rather than in ./test/downstream.sh because it is most likely to fail,
## and it's helpful to see that only it, not other downstream tests, failed.

"/tmp/$USER/plume-scripts/git-clone-related" typetools checker-framework-inference

export AFU="${AFU:-$(cd ../annotation-tools/annotation-file-utilities >/dev/null 2>&1 && pwd -P)}"
export PATH=$AFU/scripts:$PATH
(cd ../checker-framework-inference && ./.travis-build.sh)
