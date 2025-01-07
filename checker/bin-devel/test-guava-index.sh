#!/bin/bash
# Use `bash` instead of `sh` because of use of BASH_SOURCE.

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
export ORG_GRADLE_PROJECT_useJdk17Compiler=true
source "$SCRIPTDIR"/clone-related.sh


"$SCRIPTDIR/.git-scripts/git-clone-related" typetools guava
cd ../guava

if [ "$TRAVIS" = "true" ] ; then
  # Travis which kills jobs that have not produced output for 10 minutes.
  echo "Setting up sleep-and-output jobs for Travis"
  (sleep 1 && echo "1 second has elapsed") &
  (sleep 300 && echo "5 minutes have elapsed") &
  (sleep 840 && echo "14 minutes have elapsed") &
  (sleep 1380 && echo "23 minutes have elapsed") &
fi

./typecheck.sh index
