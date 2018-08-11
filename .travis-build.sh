#!/bin/bash

echo "Entering checker-framework/.travis-build.sh, GROUP=$1, in" `pwd`

# Optional argument $1 is one of:
#   all, all-tests, jdk.jar, checker-framework-inference, downstream, misc, plume-lib
# It defaults to "all".
export GROUP=$1
if [[ "${GROUP}" == "" ]]; then
  export GROUP=all
fi

if [[ "${GROUP}" != "all" && "${GROUP}" != "all-tests" && "${GROUP}" != "jdk.jar" && "${GROUP}" != "checker-framework-inference" && "${GROUP}" != "downstream" && "${GROUP}" != "misc" && "${GROUP}" != "plume-lib" ]]; then
  echo "Bad argument '${GROUP}'; should be omitted or one of: all, all-tests, jdk.jar, checker-framework-inference, downstream, misc, plume-lib."
  exit 1
fi

# Optional argument $2 is one of:
#  downloadjdk, buildjdk
# If it is omitted, this script uses downloadjdk.
export BUILDJDK=$2
if [[ "${BUILDJDK}" == "" ]]; then
  export BUILDJDK=buildjdk
fi

if [[ "${BUILDJDK}" != "buildjdk" && "${BUILDJDK}" != "downloadjdk" ]]; then
  echo "Bad argument '${BUILDJDK}'; should be omitted or one of: downloadjdk, buildjdk."
  exit 1
fi

# Fail the whole script if any command fails
set -e


## Diagnostic output
# Output lines of this script as they are read.
set -o verbose
# Output expanded lines of this script as they are executed.
set -o xtrace

export SHELLOPTS

SLUGOWNER=${TRAVIS_REPO_SLUG%/*}
if [[ "$SLUGOWNER" == "" ]]; then
  SLUGOWNER=eisop
fi

export CHECKERFRAMEWORK=`readlink -f ${CHECKERFRAMEWORK:-.}`
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

source ./.travis-build-without-test.sh ${BUILDJDK}
# The above command builds or downloads the JDK, so there is no need for a
# subsequent command to build it except to test building it.

set -e

echo "In checker-framework/.travis-build.sh GROUP=$GROUP"

if [[ "${GROUP}" == "plume-lib" || "${GROUP}" == "all" ]]; then
  # plume-lib-typecheck: 15 minutes
  [ -d /tmp/plume-scripts ] || (cd /tmp && git clone --depth 1 https://github.com/plume-lib/plume-scripts.git)
  REPO=`/tmp/plume-scripts/git-find-fork ${SLUGOWNER} typetests plume-lib-typecheck`
  BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${TRAVIS_PULL_REQUEST_BRANCH:-$TRAVIS_BRANCH}`
  (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 ${REPO}) || (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 ${REPO})

  (cd ../plume-lib-typecheck && ./.travis-build.sh)
fi

if [[ "${GROUP}" == "all-tests" || "${GROUP}" == "all" ]]; then
  ./gradlew --console=plain allTests
  # Moved example-tests-nobuildjdk out of all tests because it fails in
  # the release script because the newest maven artifacts are not published yet.
  ./gradlew --console=plain :checker:exampleTests
fi

if [[ "${GROUP}" == "checker-framework-inference" || "${GROUP}" == "all" ]]; then
  ## checker-framework-inference is a downstream test, but run it in its
  ## own group because it is most likely to fail, and it's helpful to see
  ## that only it, not other downstream tests, failed.

  # checker-framework-inference: 18 minutes
  [ -d /tmp/plume-scripts ] || (cd /tmp && git clone --depth 1 https://github.com/plume-lib/plume-scripts.git)
  REPO=`/tmp/plume-scripts/git-find-fork ${SLUGOWNER} typetools checker-framework-inference`
  BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${TRAVIS_PULL_REQUEST_BRANCH:-$TRAVIS_BRANCH}`
  (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 ${REPO}) || (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 ${REPO})

  export AFU=`readlink -f ${AFU:-../annotation-tools/annotation-file-utilities}`
  export PATH=$AFU/scripts:$PATH
  (cd ../checker-framework-inference && ./gradlew --console=plain dist test)

fi

if [[ "${GROUP}" == "downstream" || "${GROUP}" == "all" ]]; then
  ## downstream tests:  projects that depend on the Checker Framework.
  ## These are here so they can be run by pull requests.  (Pull requests
  ## currently don't trigger downstream jobs.)
  ## Not done in the Travis build, but triggered as a separate Travis project:
  ##  * daikon-typecheck: (takes 2 hours)

  # Checker Framework demos
  if [[ "${BUILDJDK}" = "downloadjdk" ]]; then
    ## If buildjdk, use "demos" below:
    ##  * checker-framework.demos (takes 15 minutes)
    ./gradlew --console=plain :checker:demosTests
  fi

  # sparta: 1 minute, but the command is "true"!
  # TODO: requires Android installation (and at one time, it caused weird
  # Travis hangs if enabled without Android installation).
  # (cd .. && git clone --depth 1 https://github.com/${SLUGOWNER}/sparta.git)
  # (cd ../sparta && ant jar all-tests)

  # Guava
  echo "Running:  (cd .. && git clone --depth 1 https://github.com/typetools/guava.git)"
  (cd .. && git clone https://github.com/typetools/guava.git) || (cd .. && git clone https://github.com/typetools/guava.git)
  echo "... done: (cd .. && git clone --depth 1 https://github.com/typetools/guava.git)"
  export CHECKERFRAMEWORK=${CHECKERFRAMEWORK:-$ROOT/checker-framework}
  (cd $ROOT/guava/guava && mvn compile -P checkerframework-local -Dcheckerframework.checkers=org.checkerframework.checker.nullness.NullnessChecker)

fi

if [[ "${GROUP}" == "jdk.jar" || "${GROUP}" == "all" ]]; then
  ## Run the tests for the type systems that use the annotated JDK
  ./gradlew --console=plain IndexTest LockTest NullnessFbcTest OptionalTest -PuseLocalJdk
fi

if [[ "${GROUP}" == "misc" || "${GROUP}" == "all" ]]; then
  ## jdkany tests: miscellaneous tests that shouldn't depend on JDK version.
  ## (Maybe they don't even need the full ./.travis-build-without-test.sh ;
  ## for example they currently don't need the annotated JDK.)

  set -e

  # Code style and formatting
  ./gradlew --console=plain checkBasicStyle checkFormat

  # Run error-prone
  ./gradlew --console=plain runErrorProne

  # Documentation
  ./gradlew --console=plain allJavadoc
  ./gradlew --console=plain javadocPrivate
  make -C docs/manual all

  echo "TRAVIS_COMMIT_RANGE = $TRAVIS_COMMIT_RANGE"
  # (git diff $TRAVIS_COMMIT_RANGE > /tmp/diff.txt 2>&1) || true
  # The change to TRAVIS_COMMIT_RANGE is due to travis-ci/travis-ci#4596 .
  (git diff "${TRAVIS_COMMIT_RANGE/.../..}" > /tmp/diff.txt 2>&1) || true
  (./gradlew requireJavadocPrivate --console=plain > /tmp/rjp-output.txt 2>&1) || true
  [ -s /tmp/diff.txt ] || (echo "/tmp/diff.txt is empty" && false)
  wget https://raw.githubusercontent.com/plume-lib/plume-scripts/master/lint-diff.py
  python lint-diff.py --strip-diff=1 --strip-lint=2 /tmp/diff.txt /tmp/rjp-output.txt

  # jsr308-langtools documentation (it's kept at Bitbucket rather than GitHub)
  # Not just "make" because the invocations of "hevea -exec xxcharset.exe" fail.
  # I cannot reproduce the problem locally and it isn't important enough to fix.
  # make -C ../jsr308-langtools/doc
  make -C ../jsr308-langtools/doc pdf

  # HTML legality
  ./gradlew --console=plain htmlValidate

fi

echo "Exiting checker-framework/.travis-build.sh, GROUP=$GROUP, in" `pwd`
