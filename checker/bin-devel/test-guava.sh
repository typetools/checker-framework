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


## Typechecking with all type systems command completes in 30 minutes, which is
## fine for Azure but times out on Travis which kills jobs that have not
## produced output for 10 minutes.
if [ "$TRAVIS" = "true" ] ; then
  cd guava
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
