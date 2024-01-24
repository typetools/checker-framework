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

killall java || true

# Start the Gradle command in the background
./gradlew test --console=plain --warning-mode=all --no-daemon --no-parallel &
gradle_pid=$!

# Wait a bit for JVM to start
sleep 180

# Find the Java process started by Gradle. Modify the grep pattern if needed.
# This command may vary depending on how your Java process is named.
java_pid=$(jps | grep GradleWorkerMain | awk '{print $1}')

# Check if we have found the PID
if [ -z "$java_pid" ]; then
    echo "Java process not found. Exiting."
    exit 1
fi

echo "Found Java process with PID: $java_pid"

# Loop to run jstack every minute until the Gradle process ends
while kill -0 "$gradle_pid" >/dev/null 2>&1; do
    echo "Dumping stack for PID $java_pid"
    jstack "$java_pid"
    sleep 60
done

echo "Gradle process has completed."

