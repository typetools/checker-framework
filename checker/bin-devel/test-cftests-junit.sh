#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
# Test that the CF, when built with JDK 21, works on other JDKs.
mkdir ~/.gradle && echo "org.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64" >> ~/.gradle/gradle.properties

source "$SCRIPT_DIR"/clone-related.sh

# Print the version of Java used to run Gradle. (This is for debugging.)
./gradlew printJavaVersion

./gradlew test --console=plain --warning-mode=all

# Print the version of Java used to test the Checker Framework. (This is for debugging.)
javap -v checker/tests/build/testclasses/AnoymousAndInnerClass.class | grep major
