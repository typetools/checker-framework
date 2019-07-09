#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS

git -C /tmp/plume-scripts pull > /dev/null 2>&1 \
  || git -C /tmp clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git

SLUGOWNER=`/tmp/plume-scripts/git-organization typetools`
echo SLUGOWNER=$SLUGOWNER

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

echo "TRAVIS_PULL_REQUEST_BRANCH=$TRAVIS_PULL_REQUEST_BRANCH"
echo "TRAVIS_BRANCH=$TRAVIS_BRANCH"

# Checker Framework demos
REPO=`/tmp/plume-scripts/git-find-fork ${SLUGOWNER} typetools checker-framework.demos`
BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${TRAVIS_PULL_REQUEST_BRANCH:-$TRAVIS_BRANCH}`
(cd .. && git clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO} checker-framework-demos) || (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO} checker-framework-demos)
./gradlew :checker:demosTests --console=plain --warning-mode=all --no-daemon

# Guava
REPO=`/tmp/plume-scripts/git-find-fork ${SLUGOWNER} typetools guava`
BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${TRAVIS_PULL_REQUEST_BRANCH:-$TRAVIS_BRANCH} cf-master`
if [ $BRANCH = "master" ] ; then
  # ${SLUGOWNER} has a fork of Guava, but no branch that corresponds to the pull-requested branch.
  # Use upstream instead.
  REPO=https://github.com/typetools/guava.git
  BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${TRAVIS_PULL_REQUEST_BRANCH:-$TRAVIS_BRANCH} cf-master`
  if [ $BRANCH = "master" ] ; then
    BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} cf-master master`
  fi
fi
git clone -C .. -b ${BRANCH} --single-branch --depth 1 -q ${REPO} guava || git clone -C .. -b ${BRANCH} --single-branch --depth 1 -q ${REPO} guava
(cd ../guava/guava && mvn compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.nullness.NullnessChecker)
