#!/bin/sh

rm -rf checkers.zip
wget -q http://types.cs.washington.edu/checker-framework/current/checkers.zip
rm -rf checker-framework/
unzip -q checkers.zip

echo `which java`
java -version

export CHECKERS=`pwd`/checker-framework/checkers

$CHECKERS/binary/javac -version
java -Xbootclasspath/p:$CHECKERS/binary/jsr308-all.jar \
       -jar $CHECKERS/binary/jsr308-all.jar -version

$CHECKERS/binary/javac -processor checkers.nullness.NullnessChecker \
    $CHECKERS/examples/NullnessExample.java 

java -Xbootclasspath/p:$CHECKERS/binary/jsr308-all.jar \
    -jar $CHECKERS/binary/jsr308-all.jar \
    -processor checkers.nullness.NullnessChecker \
    $CHECKERS/examples/NullnessExample.java 