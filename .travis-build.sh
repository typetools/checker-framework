#!/bin/bash

# Fail the whole script if any command fails
set -e

export SHELLOPTS

./.travis-build-without-test.sh

## Code style
ant check-style

## Documentation
ant javadoc-private
# Skip the manual because it cannot be compiled on Ubuntu 12.04.
# make -C checker/manual all

## Tests
# The JDK was built above; there is no need to rebuild it again.
# Don't use "-d" to debug ant, because that results in a log so long
# that Travis truncates the log and terminates the job.
# Comment this out since it causes a crash.
# ant tests-nobuildjdk

## test that Travis doesn't crash on
# Run framework tests.
(cd framework && ant all-tests-nojtreg-nobuild)
# Subset of all-tests
# Also runs framework jtreg-tests
(cd checker && ant jtreg-tests)
(cd checker && ant command-line-tests)
(cd checker && ant example-tests-nobuildjdk)
(cd checker && ant check-tutorial)
# If too many checker tests are run, the tests crash, so run one.
(cd checker && ant nullness-base-tests)
## end of test that Travis doesn't crash on

#Fails on Travis
#(cd checker && ant check-compilermsgs check-purity)

# It's cheaper to run the demos test here than to trigger the
# checker-framework-demos job, which has to build the whole Checker Framework.
(cd checker && ant check-demos)
