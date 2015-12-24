#!/bin/bash

# Fail the whole script if any command fails
set -e

./.travis-build-without-test.sh

## Documentation
ant javadoc-private
# Skip the manual because it cannot be compiled on Ubuntu 12.04.
# make -C checker/manual all

## Tests
# The JDK was built above; there is no need to rebuild it again.
# Don't use "-d" to debug ant, because that results in a log so long
# that Travis truncates the log and terminates the job.
ant tests-nobuildjdk

(cd checker && ant check-compilermsgs check-purity)
(cd checker && ant check-tutorial)

# It's cheaper to run the demos test here than to trigger the
# checker-framework-demos job, which has to build the whole Checker Framework.
(cd checker && ant check-demos)
