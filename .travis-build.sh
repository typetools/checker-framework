#!/bin/bash
ROOT=$TRAVIS_BUILD_DIR/..
cd $ROOT
git clone https://github.com/typetools/annotation-tools.git
cd annotation-tools/
./.travis-build.sh
# This also builds jsr308-langtools

cd $ROOT/checker-framework
ant clean
travis_wait ant dist
travis_wait ant javadoc tests-nojdk
