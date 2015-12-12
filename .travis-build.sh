#!/bin/bash

# Fail the whole script if any command fails
set -e

./.travis-build-without-test.sh

## Documentation
ant javadoc-private
# Skip the manual because it cannot be compiled on Ubuntu 12.04.
# make -C checker/manual all

## Temporarily commented out so tests run faster
# ## Tests
# # The JDK was built above; there is no need to rebuild it again.
# ant tests-nobuildjdk

(cd checker && ant check-compilermsgs check-purity)
(cd checker && ant check-tutorial)

# It's cheaper to run the demos test here than to trigger the
# checker-framework-demos job, which has to build the whole Checker Framework.
# It would be better for "ant check-demos" to set the environment
# appropriately, but it's expedient to do it here.
export CHECKERFRAMEWORK=$TRAVIS_BUILD_DIR
(cd checker && ant check-demos)
