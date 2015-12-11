#!/bin/bash

./.travis-build-without-test.sh

## Documentation
ant javadoc-private
make -C checker/manual all

## Tests
# The JDK was built above; there is no need to rebuild it again.
ant tests-nobuildjdk

(cd checker && ant check-compilermsgs check-purity)
(cd checker && ant check-tutorial)
