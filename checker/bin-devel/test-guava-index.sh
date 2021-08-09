#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
# shellcheck disable=SC1090 # In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
source "$SCRIPTDIR"/build.sh


"$SCRIPTDIR/.plume-scripts/git-clone-related" typetools guava
cd ../guava

if [ "$TRAVIS" = "true" ] ; then
  # Travis which kills jobs that have not produced output for 10 minutes.
  echo "Setting up sleep-and-output jobs for Travis"
  (sleep 1s && echo "1 second has elapsed") &
  (sleep 5m && echo "5 minutes have elapsed") &
  (sleep 14m && echo "14 minutes have elapsed") &
  (sleep 23m && echo "23 minutes have elapsed") &
fi

./typecheck.sh index
