#!/bin/bash

set -e
# set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
# Test that the CF, when built with JDK 21, works on other JDKs.
export ORG_GRADLE_PROJECT_useJdk21Compiler=true

# Run Gradle using Java 21.
mkdir ~/.gradle && echo "org.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64" >> ~/.gradle/gradle.properties

source "$SCRIPT_DIR"/clone-related.sh

echo "IS_CI=$IS_CI"

java -XX:+PrintFlagsFinal -version | grep HeapSize
echo JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS"
echo JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS"
echo _JAVA_OPTIONS="$_JAVA_OPTIONS"
echo DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS"
echo GRADLE_OPTS="$GRADLE_OPTS"
echo JAVA_OPTS="$JAVA_OPTS"

./gradlew test --warning-mode=all
