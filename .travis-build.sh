#!/bin/bash

# Optional argument $1 is one of:
#   all, junit, nonjunit, all-tests, jdk.jar, demos, downstream, misc
# If it is omitted, this script does everything.
export GROUP=$1
if [[ "${GROUP}" == "" ]]; then
  export GROUP=all
fi

if [[ "${GROUP}" != "all" && "${GROUP}" != "junit" && "${GROUP}" != "nonjunit" && "${GROUP}" != "all-tests" && "${GROUP}" != "jdk.jar" && "${GROUP}" != "demos" && "${GROUP}" != "downstream" && "${GROUP}" != "misc" ]]; then
  echo "Bad argument '${GROUP}'; should be omitted or one of: all, junit, nonjunit, all-tests, jdk.jar, demos, downstream, misc."
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
# Don't use "-d" to debug ant, because that results in a log so long
# that Travis truncates the log and terminates the job.

export SHELLOPTS

SLUGOWNER=${TRAVIS_REPO_SLUG%/*}
if [[ "$SLUGOWNER" == "" ]]; then
  SLUGOWNER=typetools
fi

./.travis-build-without-test.sh ${BUILDJDK}
# The above command builds or downloads the JDK, so there is no need for a
# subsequent command to build it except to test building it.

set -e

# Subsumed by "all-tests" group.
if [[ "${GROUP}" == "junit" || "${GROUP}" == "all" ]]; then
  (cd checker && ant junit-tests-nojtreg-nobuild)
fi

# Subsumed by "all-tests" group.
if [[ "${GROUP}" == "nonjunit" || "${GROUP}" == "all" ]]; then
  (cd checker && ant nonjunit-tests-nojtreg-nobuild jtreg-tests)
fi

if [[ "${GROUP}" == "all-tests" || "${GROUP}" == "all" ]]; then
  (cd checker && ant all-tests-nobuildjdk)
  # Moved example-tests-nobuildjdk out of all tests because it fails in
  # the release script because the newest maven artifacts are not published yet.
  (cd checker && ant example-tests-nobuildjdk)
  # If the above command ever exceeds the time limit on Travis, it can be split
  # using the following commands:
  # (cd checker && ant junit-tests-nojtreg-nobuild)
  # (cd checker && ant nonjunit-tests-nojtreg-nobuild jtreg-tests)
fi

if [[ "${GROUP}" == "downstream" || "${GROUP}" == "all" ]]; then
  ## downstream tests:  projects that depend on the Checker Framework.
  ## These are here so they can be run by pull requests.  (Pull requests
  ## currently don't trigger downstream jobs.)
  ## Not done in the Travis build, but triggered as a separate Travis project:
  ##  * daikon-typecheck: (takes 2 hours)

  # checker-framework-inference: 18 minutes
  set +e
  echo "Running: git ls-remote https://github.com/${SLUGOWNER}/checker-framework-inference.git &>-"
  git ls-remote https://github.com/${SLUGOWNER}/checker-framework-inference.git &>-
  if [ "$?" -ne 0 ]; then
    CFISLUGOWNER=typetools
  else
    CFISLUGOWNER=${SLUGOWNER}
  fi
  set -e
  echo "Running:  (cd .. && git clone --depth 1 https://github.com/${CFISLUGOWNER}/checker-framework-inference.git)"
  (cd .. && git clone --depth 1 https://github.com/${CFISLUGOWNER}/checker-framework-inference.git)
  echo "... done: (cd .. && git clone --depth 1 https://github.com/${CFISLUGOWNER}/checker-framework-inference.git)"

  export AFU=`pwd`/../annotation-tools/annotation-file-utilities
  export PATH=$AFU/scripts:$PATH
  (cd ../checker-framework-inference && gradle dist && ant -f tests.xml run-tests)

  # plume-lib-typecheck: 30 minutes
  set +e
  echo "Running: git ls-remote https://github.com/${SLUGOWNER}/plume-lib.git &>-"
  git ls-remote https://github.com/${SLUGOWNER}/plume-lib.git &>-
  if [ "$?" -ne 0 ]; then
    PLSLUGOWNER=mernst
  else
    PLSLUGOWNER=${SLUGOWNER}
  fi
  set -e
  echo "Running:  (cd .. && git clone --depth 1 https://github.com/${PLSLUGOWNER}/plume-lib.git)"
  (cd .. && git clone https://github.com/${PLSLUGOWNER}/plume-lib.git)
  echo "... done: (cd .. && git clone --depth 1 https://github.com/${PLSLUGOWNER}/plume-lib.git)"

  export CHECKERFRAMEWORK=`pwd`
  (cd ../plume-lib/java && make check-types)

  if [[ "${BUILDJDK}" = "downloadjdk" ]]; then
    ## If buildjdk, use "demos" below:
    ##  * checker-framework.demos (takes 15 minutes)
    (cd checker && ant check-demos)
  fi
  # sparta: 1 minute, but the command is "true"!
  # TODO: requires Android installation (and at one time, it caused weird
  # Travis hangs if enabled without Android installation).
  # (cd .. && git clone --depth 1 https://github.com/${SLUGOWNER}/sparta.git)
  # (cd ../sparta && ant jar all-tests)

fi

if [[ "${GROUP}" == "demos" || "${GROUP}" == "all" ]]; then
  (cd checker && ant check-demos)
fi

if [[ "${GROUP}" == "jdk.jar" || "${GROUP}" == "all" ]]; then
  cd checker
  ant jdk.jar
  ## Run the tests for the type systems that use the annotated JDK
  ant index-tests lock-tests nullness-tests-nobuildjdk
fi

if [[ "${GROUP}" == "misc" || "${GROUP}" == "all" ]]; then
  ## jdkany tests: miscellaneous tests that shouldn't depend on JDK version.
  ## (Maybe they don't even need the full ./.travis-build-without-test.sh ;
  ## for example they currently don't need the annotated JDK.)

  set -e

  # Code style and formatting
  ant -d check-style
  release/checkPluginUtil.sh

  # Run error-prone
  (cd checker; ant check-errorprone)

  # Documentation
  ant javadoc-private
  make -C docs/manual all

  # jsr308-langtools documentation (it's kept at Bitbucket rather than GitHub)
  # Not just "make" because the invocations of "hevea -exec xxcharset.exe" fail.
  # I cannot reproduce the problem locally and it isn't important enough to fix.
  # make -C ../jsr308-langtools/doc
  make -C ../jsr308-langtools/doc pdf

  # HTML legality
  ant html-validate

fi
