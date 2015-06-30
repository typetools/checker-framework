#!/bin/bash

rm -rf checker-framework.zip
wget -q http://types.cs.washington.edu/checker-framework/current/checker-framework.zip
rm -rf checker-framework-*/
unzip -q checker-framework.zip

export CHECKERFRAMEWORK=`pwd`/`ls -d checker-framework-*`
export ORIG_PATH=$PATH


function cfruntest() {
  echo `which java`
  java -version

  $CHECKERFRAMEWORK/checker/bin/javac -version
  if (($?)); then exit 6; fi

  java -jar $CHECKERFRAMEWORK/checker/dist/checker.jar -version
  if (($?)); then exit 6; fi

  $CHECKERFRAMEWORK/checker/bin/javac -processor org.checkerframework.checker.nullness.NullnessChecker \
      $CHECKERFRAMEWORK/checker/examples/NullnessReleaseTests.java
  if (($?)); then exit 6; fi

  java -jar $CHECKERFRAMEWORK/checker/dist/checker.jar \
      -processor org.checkerframework.checker.nullness.NullnessChecker \
      $CHECKERFRAMEWORK/checker/examples/NullnessReleaseTests.java
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


# echo "Testing with latest type-annotations build:"

# export JAVA_HOME=$WORKSPACE/../../type-annotations/lastSuccessful/archive/build/linux-x86_64-normal-server-release/images/j2sdk-image
# export PATH=$JAVA_HOME/bin:$ORIG_PATH

# cfruntest
