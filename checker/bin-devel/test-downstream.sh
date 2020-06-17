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


## downstream tests:  projects that depend on the Checker Framework.
## These are here so they can be run by pull requests.  (Pull requests
## currently don't trigger downstream jobs.)
## Exceptions:
##  * checker-framework-inference is run by test-cf-inference.sh
##  * plume-lib is run by test-plume-lib.sh
##  * daikon-typecheck is run as a separate CI project
##  * guava is run as a separate CI project

# Checker Framework demos
"/tmp/$USER/plume-scripts/git-clone-related" typetools checker-framework.demos
./gradlew :checker:demosTests --console=plain --warning-mode=all --no-daemon
