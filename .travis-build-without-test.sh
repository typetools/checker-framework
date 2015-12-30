#!/bin/bash
ROOT=$TRAVIS_BUILD_DIR/..

# Fail the whole script if any command fails
set -e

## Build annotation-tools (Annotation File Utilities)
(cd $ROOT && (git -C annotation-tools || git clone https://github.com/typetools/annotation-tools.git))
# This also builds jsr308-langtools
(cd $ROOT/annotation-tools/ && ./.travis-build-without-test.sh)

## Compile
ant dist
