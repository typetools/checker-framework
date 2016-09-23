#!/bin/bash

# Optional argument $1 is one of:  junit, nonjunit, misc, downstream
# If it is omitted, this script does everything.
#

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

if [[ "$1" != "junit" && "$1" != "nonjunit" && "$1" != "downstream" ]]; then
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

if [[ "$1" != "nonjunit" && "$1" != "misc" && "$1" != "downstream" ]]; then
  (cd checker && ant junit-tests-nojtreg-nobuild)
fi

if [[ "$1" != "junit" && "$1" != "misc" && "$1" != "downstream" ]]; then
  (cd checker && ant nonjunit-tests-nojtreg-nobuild jtreg-tests)

  # It's cheaper to run the demos test here than to trigger the
  # checker-framework-demos job, which has to build the whole Checker Framework.
  (cd checker && ant check-demos)
fi

if [[ "$1" != "junit" && "$1" != "nonjunit" && "$1" != "misc" ]]; then
  ## downstream tests:  projects that depend on the the Checker Framework.
  ## These are here so they can be run by pull requests.  (Pull requests
  ## currently don't trigger downstream jobs.)

  # TODO
  # checker-framework-demos: 15 minutes
  # checker-framework-inference: 18 minutes
  # daikon-typecheck: a long time, already split into multiple jobs
  # plume-lib-typecheck: 30 minutes
  # sparta: 1 minute, but the command is "true"!
  echo "Not running downstream jobs yet"

fi
