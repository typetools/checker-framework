#!/bin/bash

# Optional argument $1 is one of:
#   all, junit, nonjunit, downstream, plume-lib-typecheck, misc
# If it is omitted, this script does everything.

export GROUP=$1
if [[ "${GROUP}" == "" ]]; then
  export GROUP=all
fi

if [[ "${GROUP}" != "all" && "${GROUP}" != "misc" && "${GROUP}" != "junit" && "${GROUP}" != "nonjunit" && "${GROUP}" != "downstream" && "${GROUP}" != "plume-lib-typecheck" ]]; then
  echo "Bad argument '${GROUP}'; should be omitted or one of: all, misc, junit, nonjunit, downstream, plume-lib-typecheck."
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


./.travis-build-without-test.sh
# The above command builds the JDK, so there is no need for a subsequent
# command to rebuild it again.

if [[ "${GROUP}" == "junit" || "${GROUP}" == "all" ]]; then
  (cd checker && ant junit-tests-nojtreg-nobuild)
fi

if [[ "${GROUP}" == "nonjunit" || "${GROUP}" == "all" ]]; then
  (cd checker && ant nonjunit-tests-nojtreg-nobuild jtreg-tests)

  # It's cheaper to run the demos test here than to trigger the
  # checker-framework-demos job, which has to build the whole Checker Framework.
  (cd checker && ant check-demos)
fi

if [[ "${GROUP}" == "downstream" || "${GROUP}" == "all" ]]; then
  ## downstream tests:  projects that depend on the the Checker Framework.
  ## These are here so they can be run by pull requests.  (Pull requests
  ## currently don't trigger downstream jobs.)
  ## Not done in the main "downstream" job, but in its own job to avoid timeouts:
  ##  * plume-lib-typecheck (takes 30 minutes)
  ## Not done in the Travis build, but triggered as a separate Travis project:
  ##  * daikon-typecheck: (takes 2 hours)

  # checker-framework-demos: 15 minutes
  (cd .. && git clone --depth 1 https://github.com/typetools/checker-framework.demos.git)
  (cd ../checker-framework.demos && ant -Djsr308.home=$ROOT)

  # checker-framework-inference: 18 minutes
  (cd .. && git clone --depth 1 https://github.com/typetools/checker-framework-inference.git)
  export AFU=`pwd`/../annotation-tools/annotation-file-utilities
  export PATH=$AFU/scripts:$PATH
  (cd ../checker-framework-inference && gradle dist && ant -f tests.xml run-tests)

  # sparta: 1 minute, but the command is "true"!
  # TODO: requires Android installation (and at one time, it caused weird
  # Travis hangs if enabled without Android installation).
  # (cd .. && git clone --depth 1 https://github.com/typetools/sparta.git)
  # (cd ../sparta && ant jar all-tests)
fi

if [[ "${GROUP}" == "plume-lib-typecheck" || "${GROUP}" == "all" ]]; then
  (cd .. && git clone https://github.com/mernst/plume-lib.git)
  export CHECKERFRAMEWORK=`pwd`
  (cd ../plume-lib/java && make check-types)
fi

if [[ "${GROUP}" == "misc" || "${GROUP}" == "all" ]]; then
  ## jdkany tests: miscellaneous tests that shouldn't depend on JDK version.
  ## (Maybe they don't even need the full ./.travis-build-without-test.sh ;
  ## for example they currently don't need the annotated JDK.)

  # Code style and formatting
  ant check-style
  release/checkPluginUtil.sh

  # Documentation
  ant javadoc-private
  make -C checker/manual all
fi
