#!/bin/bash

rm -rf checkers.zip
wget -q http://types.cs.washington.edu/checker-framework/current/checkers.zip
rm -rf checker-framework-*/
unzip -q checkers.zip

export CHECKERS=`pwd`/`ls -d checker-framework-*`
export ORIG_PATH=$PATH


function cfruntest() {
  echo `which java`
  java -version

  $CHECKERS/binary/javac -version
  if (($?)); then exit 6; fi

  java -jar $CHECKERS/binary/checkers.jar -version
  if (($?)); then exit 6; fi

  $CHECKERS/binary/javac -processor checkers.nullness.NullnessChecker \
      $CHECKERS/examples/NullnessReleaseTests.java 
  if (($?)); then exit 6; fi

  java -jar $CHECKERS/binary/checkers.jar \
      -processor checkers.nullness.NullnessChecker \
      $CHECKERS/examples/NullnessReleaseTests.java 
  if (($?)); then exit 6; fi
}

echo "Testing with Java 7:"

export JAVA_HOME=$JAVA7_HOME
export PATH=$JAVA_HOME/bin:$ORIG_PATH

cfruntest


echo "Testing with Java 8 build:"

export JAVA_HOME=$JAVA8_HOME
export PATH=$JAVA_HOME/bin:$ORIG_PATH

cfruntest

echo "Testing with latest type-annotations build:"

export JAVA_HOME=$WORKSPACE/../../type-annotations/lastSuccessful/archive/build/linux-x86_64-normal-server-release/images/j2sdk-image
export PATH=$JAVA_HOME/bin:$ORIG_PATH

cfruntest
