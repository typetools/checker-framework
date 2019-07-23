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


## downstream tests:  projects that depend on the Checker Framework.
## These are here so they can be run by pull requests.  (Pull requests
## currently don't trigger downstream jobs.)
## Exceptions:
##  * checker-framework-inference is run by test-cf-inference.sh
##  * plume-lib is run by test-plume-lib.sh
##  * daikon-typecheck is run as a separate CI project

# Checker Framework demos
REPO=`/tmp/plume-scripts/git-find-fork ${CI_ORGANIZATION} typetools checker-framework.demos`
BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${CI_BRANCH}`
(cd .. && git clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO} checker-framework-demos) || (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO} checker-framework-demos)
./gradlew :checker:demosTests --console=plain --warning-mode=all --no-daemon

# Guava
REPO=`/tmp/plume-scripts/git-find-fork ${CI_ORGANIZATION} typetools guava`
BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${CI_BRANCH} cf-master`
if [ $BRANCH = "master" ] ; then
  # ${CI_ORGANIZATION} has a fork of Guava, but no branch that corresponds to the pull-requested branch.
  # Use upstream instead.
  REPO=https://github.com/typetools/guava.git
  BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${CI_BRANCH} cf-master`
  if [ $BRANCH = "master" ] ; then
    BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} cf-master master`
  fi
fi
git -C .. clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO} guava || git -C .. clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO} guava
(cd ../guava/guava && mvn compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.nullness.NullnessChecker)
