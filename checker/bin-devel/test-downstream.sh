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

## downstream tests:  projects that depend on the Checker Framework.
## (There are none currently in this file.)
## These are here so they can be run by pull requests.
## Exceptions:
##  * plume-lib is run by test-plume-lib.sh
##  * daikon-typecheck is run as a separate CI project
##  * guava is run as a separate CI project
