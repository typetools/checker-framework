#!/bin/bash

rm -rf checkers.zip
wget -q http://types.cs.washington.edu/checker-framework/current/checkers.zip
rm -rf checker-framework/
unzip -q checkers.zip

export CHECKERS=`pwd`/checker-framework/checkers
export ORIG_PATH=$PATH


function cfruntest() {
  echo `which java`
  java -version

  $CHECKERS/binary/javac -version
  if (($?)); then exit 6; fi

  java -Xbootclasspath/p:$CHECKERS/binary/jsr308-all.jar \
      -jar $CHECKERS/binary/jsr308-all.jar -version
  if (($?)); then exit 6; fi

  $CHECKERS/binary/javac -processor checkers.nullness.NullnessChecker \
      $CHECKERS/examples/NullnessReleaseTests.java 
  if (($?)); then exit 6; fi

  java -Xbootclasspath/p:$CHECKERS/binary/jsr308-all.jar \
      -jar $CHECKERS/binary/jsr308-all.jar \
      -processor checkers.nullness.NullnessChecker \
      $CHECKERS/examples/NullnessReleaseTests.java 
  if (($?)); then exit 6; fi
}


echo "Testing with Java 6:"

export JAVA_HOME=$JAVA6_HOME
export PATH=$JAVA_HOME/bin:$PATH

cfruntest


echo "Testing with Java 7:"

export JAVA_HOME=$JAVA7_HOME
export PATH=$JAVA_HOME/bin:$ORIG_PATH

cfruntest