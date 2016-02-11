#!/bin/bash

# Fail the whole script if any command fails
set -e

export SHELLOPTS

## Build annotation-tools (Annotation File Utilities)
(cd .. && (git -C annotation-tools pull || git clone https://github.com/typetools/annotation-tools.git))
# This also builds jsr308-langtools
(cd ../annotation-tools/ && ./.travis-build-without-test.sh)

## Compile
ant dist
