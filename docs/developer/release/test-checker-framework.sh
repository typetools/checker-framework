#!/bin/bash

# This script tests that the Checker Framework release can be downloaded
# and that a simple sanity test works.
# It assumes that environment variable JAVA8_HOME is defined.
# It takes an argument specifying the current Checker Framework version, e.g. "1.9.11".

# This script is used by the release_push script in the "Run javac sanity tests on the live release" step

set -x

if [ $# -eq 0 ]; then
    echo "Usage: test-checker-framework.sh <current version of Checker Framework on live web site>"
    exit 6
fi

rm -f checker-framework-$1.zip
rm -rf checker-framework-$1/

wget https://checkerframework.org/checker-framework-$1.zip
unzip -q checker-framework-$1.zip

export CHECKERFRAMEWORK=checker-framework-$1
export ORIG_PATH=$PATH


function cfruntest() {
  echo `which java`
  java -version

  chmod +x $CHECKERFRAMEWORK/checker/bin/javac
  $CHECKERFRAMEWORK/checker/bin/javac -version
  if (($?)); then exit 6; fi

  java -jar "$CHECKERFRAMEWORK/checker/dist/checker.jar" -version
  if (($?)); then exit 6; fi

  "$CHECKERFRAMEWORK/checker/bin/javac" -processor org.checkerframework.checker.nullness.NullnessChecker \
      "$CHECKERFRAMEWORK/docs/examples/NullnessReleaseTests.java"
  if (($?)); then exit 6; fi

  java -jar "$CHECKERFRAMEWORK/checker/dist/checker.jar" \
      -processor org.checkerframework.checker.nullness.NullnessChecker \
      "$CHECKERFRAMEWORK/docs/examples/NullnessReleaseTests.java"
  if (($?)); then exit 6; fi
}

echo "Testing with Java 8:"

export JAVA_HOME=$JAVA8_HOME
export PATH=$JAVA_HOME/bin:$ORIG_PATH

cfruntest


# echo "Testing with latest type-annotations build:"

# export JAVA_HOME=$WORKSPACE/../../type-annotations/lastSuccessful/archive/build/linux-x86_64-normal-server-release/images/j2sdk-image
# export PATH=$JAVA_HOME/bin:$ORIG_PATH

# cfruntest
