#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
# shellcheck disable=SC1090 # In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
export ORG_GRADLE_PROJECT_useJdk17Compiler=true
source "$SCRIPTDIR"/clone-related.sh


## downstream tests:  projects that depend on the Checker Framework.
## (There are none currently in this file.)
## These are here so they can be run by pull requests.
## Exceptions:
##  * plume-lib is run by test-plume-lib.sh
##  * daikon-typecheck is run as a separate CI project
##  * guava is run as a separate CI project
