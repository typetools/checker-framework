#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

if [ -d "/tmp/plume-scripts" ] ; then
  (cd /tmp/plume-scripts && git pull -q)
else
  (cd /tmp && git clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git)
fi

export CHECKERFRAMEWORK="${CHECKERFRAMEWORK:-$(pwd -P)}"
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
echo "BUILDJDK=${BUILDJDK}"
# In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
# shellcheck disable=SC1090
source "$SCRIPTDIR"/build.sh "${BUILDJDK}"


## downstream tests:  projects that depend on the Checker Framework.
## These are here so they can be run by pull requests.  (Pull requests
## currently don't trigger downstream jobs.)
## Exceptions:
##  * checker-framework-inference is run by test-cf-inference.sh
##  * plume-lib is run by test-plume-lib.sh
##  * daikon-typecheck is run as a separate CI project

# Checker Framework demos
/tmp/plume-scripts/git-clone-related typetools checker-framework.demos
./gradlew :checker:demosTests --console=plain --warning-mode=all --no-daemon

# Guava
# Can't use `git-clone-related` here, since we want slightly different behavior.
# shellcheck disable=SC2046
eval $(/tmp/plume-scripts/ci-info typetools)
REPO_URL=$(/tmp/plume-scripts/git-find-fork "${CI_ORGANIZATION}" typetools guava)
BRANCH=$(/tmp/plume-scripts/git-find-branch "${REPO_URL}" "${CI_BRANCH}" cf-master)
if [ "$BRANCH" = "master" ] ; then
  # ${CI_ORGANIZATION} has a fork of Guava, but no branch that corresponds to the pull-requested branch,
  # nor a cf-master branch.  Use upstream.
  REPO_URL=https://github.com/typetools/guava.git
  if [ "$CI_BRANCH" = "master" ] ; then
    BRANCH=$(/tmp/plume-scripts/git-find-branch ${REPO_URL} cf-master)
  else
    BRANCH=$(/tmp/plume-scripts/git-find-branch ${REPO_URL} "${CI_BRANCH}" cf-master)
  fi
fi
git -C .. clone -b "${BRANCH}" --single-branch --depth 1 -q ${REPO_URL} guava || git -C .. clone -b "${BRANCH}" --single-branch --depth 1 -q ${REPO_URL} guava
(cd ../guava/guava && mvn -B compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.nullness.NullnessChecker)
