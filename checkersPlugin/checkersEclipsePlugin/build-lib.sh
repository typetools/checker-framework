#!/bin/bash

# Updates the lib/ directory with newer versions of checkers.jar and javac.jar.

CHECKERS=../../checkers
LANGTOOLS=../../../jsr308-langtools

mkdir -p lib

# Checkers file
ant -f $CHECKERS/build.xml
cp $CHECKERS/checkers.jar lib

# langtools
ant -f $LANGTOOLS/make/build.xml clean build-javac
cp $LANGTOOLS/dist/lib/javac.jar lib
