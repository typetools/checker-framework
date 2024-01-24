#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
# shellcheck disable=SC1090# In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
export ORG_GRADLE_PROJECT_useJdk17Compiler=true
source "$SCRIPTDIR"/clone-related.sh

pids=$(jps | grep Gradle | awk '{print $1}')

for pid in $pids; do
    kill -9 "$pid"
done

# Start the Gradle command in the background
./gradlew test --console=plain --warning-mode=all --no-daemon --no-parallel &
gradle_pid=$!

# Wait a bit for JVM to start
sleep 180

# Find the Java process started by Gradle. Modify the grep pattern if needed.
# This command may vary depending on how your Java process is named.

# Check if we have found the PID

# Loop to run jstack every minute until the Gradle process ends
while kill -0 "$gradle_pid" >/dev/null 2>&1; do
    # print all Java processes first to help with debugging
    jps
    java_pid=$(jps | grep GradleWorkerMain | awk '{print $1}' | sort -n | tail -n 1)
    if [ -n "$java_pid" ]; then
        echo "Dumping stack for PID $java_pid"
        jstack "$java_pid" || true
    fi
    sleep 60
done

echo "Gradle process has completed."

