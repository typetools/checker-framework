#!/bin/bash

# Fail the whole script if any command fails
set -e
# Output lines of this script as they are read.
set -o verbose
# Output expanded lines of this script as they are executed.
set -o xtrace

export SHELLOPTS

./.travis-build-without-test.sh

# Optional argument $1 is one of:  jdk7, jdk8, jdkany
# If it is omitted, this script does everything.

if [[ "$1" != "jdk7" && "$1" != "jdk8" ]]; then
  ## jdkany tests: miscellaneous tests that shouldn't depend on JDK version.
  ## (Maybe they don't even need the full ./.travis-build-without-test.sh .)

  # Code style
  ant check-style

  # Documentation
  ant javadoc-private
  make -C checker/manual all
fi

if [[ "$1" != "jdkany" ]]; then
  ## jdk7 or jdk8 tests: the real tests

  # The JDK was built already; there is no need to rebuild it again.
  # Don't use "-d" to debug ant, because that results in a log so long
  # that Travis truncates the log and terminates the job.

  # ant tests-nobuildjdk
  ## Slightly more efficient than "ant tests-nobuildjdk", maybe:
  ant all-tests-nojtreg-nobuild jtreg-tests

  # It's cheaper to run the demos test here than to trigger the
  # checker-framework-demos job, which has to build the whole Checker Framework.
  (cd checker && ant check-demos)
fi
