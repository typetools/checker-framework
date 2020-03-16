#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

if [ -d "/tmp/plume-scripts" ] ; then
  (cd /tmp/plume-scripts && git pull -q)
else
  (cd /tmp && (git clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git || git clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git))
fi

export CHECKERFRAMEWORK="${CHECKERFRAMEWORK:-$(pwd -P)}"
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
echo "BUILDJDK=${BUILDJDK}"
# In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
# shellcheck disable=SC1090
source "$SCRIPTDIR"/build.sh "${BUILDJDK}"


/tmp/plume-scripts/git-clone-related typetools guava
cd ../guava

if [ "$TRAVIS" = "true" ] ; then
  # Keep Travis from killing the job due to too much time without output
  echo "Setting up sleep-and-output jobs for Travis"
  (sleep 1s && echo "1 second has elapsed") &
  (sleep 5m && echo "5 minutes have elapsed") &
  (sleep 14m && echo "14 minutes have elapsed") &
  (sleep 23m && echo "23 minutes have elapsed") &
  (sleep 32m && echo "32 minutes have elapsed") &
  (sleep 41m && echo "41 minutes have elapsed") &
fi

## This command works locally, but on Azure it fails with timouts while downloading Maven dependencies.
# cd guava && time mvn --debug -B package -P checkerframework-local -Dmaven.test.skip=true -Danimal.sniffer.skip=true

## Typechecking with all type systems command completes in 30 minutes, which is
## fine for Azure but times out on Travis which kills jobs that have not
## produced output for 10 minutes.
if [ "$TRAVIS" = "true" ] ; then
  ./typecheck.sh formatter
  ./typecheck.sh index
  ./typecheck.sh interning
  ./typecheck.sh lock
  ./typecheck.sh nullness
  ./typecheck.sh regex
  ./typecheck.sh signature
else
  ## This command works locally, but on Azure it fails with timouts while downloading Maven dependencies.
  # cd guava && time mvn --debug -B package -P checkerframework-local -Dmaven.test.skip=true -Danimal.sniffer.skip=true

  cd guava && time mvn --debug -B compile -P checkerframework-local
fi
