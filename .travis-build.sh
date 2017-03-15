#!/bin/bash

# Optional argument $1 is one of:
#   all, junit, nonjunit, downstream, misc
# If it is omitted, this script does everything.
export GROUP=$1

# Optional argument $2 is one of:
#   jdk7, jdk8, jdkany
export JDKVER=$2

if [[ "${GROUP}" == "" ]]; then
  export GROUP=all
fi

if [[ "${GROUP}" != "all" && "${GROUP}" != "all-tests" && "${GROUP}" != "jdk.jar" && "${GROUP}" != "downstream" && "${GROUP}" != "misc" ]]; then
  echo "Bad argument '${GROUP}'; should be omitted or one of: all, all-tests, jdk.jar, downstream, misc."
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


./.travis-build-without-test.sh $JDKVER
# The above command downloads the JDK, so there is no need for a subsequent
# command to build it except to test building it.

set -e

if [[ "${GROUP}" == "all-tests" || "${GROUP}" == "all" ]]; then
  (cd checker && ant all-tests-nobuildjdk)
  # If the above commond ever exceeds the time limit on Travis, it can be split
  # using the following commands:
  # (cd checker && ant junit-tests-nojtreg-nobuild)
  # (cd checker && ant nonjunit-tests-nojtreg-nobuild jtreg-tests)
fi

if [[ "${GROUP}" == "downstream" || "${GROUP}" == "all" ]]; then
  ## downstream tests:  projects that depend on the the Checker Framework.
  ## These are here so they can be run by pull requests.  (Pull requests
  ## currently don't trigger downstream jobs.)
  ## Not done in the Travis build, but triggered as a separate Travis project:
  ##  * daikon-typecheck: (takes 2 hours)

  # checker-framework-inference: 18 minutes
  (cd .. && git clone --depth 1 https://github.com/typetools/checker-framework-inference.git)
  export AFU=`pwd`/../annotation-tools/annotation-file-utilities
  export PATH=$AFU/scripts:$PATH
  (cd ../checker-framework-inference && gradle dist && ant -f tests.xml run-tests)

  # plume-lib-typecheck: 30 minutes
  (cd .. && git clone https://github.com/mernst/plume-lib.git)
  export CHECKERFRAMEWORK=`pwd`
  (cd ../plume-lib/java && make check-types)

  # sparta: 1 minute, but the command is "true"!
  # TODO: requires Android installation (and at one time, it caused weird
  # Travis hangs if enabled without Android installation).
  # (cd .. && git clone --depth 1 https://github.com/typetools/sparta.git)
  # (cd ../sparta && ant jar all-tests)

  # It's cheaper to run the demos test here than to trigger the
  # checker-framework-demos job, which has to build the whole Checker Framework.
  (cd checker && ant check-demos)
  # Here's a more verbose way to do the same thing as "ant check-demos":
  # (cd .. && git clone --depth 1 https://github.com/typetools/checker-framework.demos.git)
  # (cd ../checker-framework.demos && ant -Djsr308.home=$ROOT)
fi

if [[ "${GROUP}" == "misc" || "${GROUP}" == "all" ]]; then
  ## jdkany tests: miscellaneous tests that shouldn't depend on JDK version.
  ## (Maybe they don't even need the full ./.travis-build-without-test.sh ;
  ## for example they currently don't need the annotated JDK.)

  set -e

  # Code style and formatting
  ant -d check-style
  release/checkPluginUtil.sh

  # Documentation
  ant javadoc-private
  make -C docs/manual all

  # jsr308-langtools documentation (it's kept at Bitbucket rather than GitHub)
  # Not just "make" because the invocations of "hevea -exec xxcharset.exe" fail.
  # I cannot reproduce the problem locally and it isn't important enough to fix.
  # make -C ../jsr308-langtools/doc
  make -C ../jsr308-langtools/doc pdf

fi


if [[ "${GROUP}" == "jdk.jar" || "${GROUP}" == "all" ]]; then
  cd checker; ant jdk.jar
fi
