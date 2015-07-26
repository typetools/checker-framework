#!/bin/bash
cd $TRAVIS_BUILD_DIR/..
git clone https://github.com/typetools/annotation-tools.git
cd annotation-tools/
./.travis-build.sh
# This also builds jsr308-langtools

cd $TRAVIS_BUILD_DIR
ant clean
ant dist
ant javadoc tests
