#!/bin/sh

# This script runs the Checker Framework's whole-program type inference on a
# program. At the end, it adds annotations refining types on the program's .java
# files.

# This script receives as arguments:
# 1. Processor name
# 2. Classpath (target project's classpath)
# 3. Extra processor arguments which will be passed to the checker
# 4. List of paths to .java files in a program

# Example of usage:
#./infer-and-annotate.sh org.checkerframework.common.value.ValueChecker \
#   $PROJECT_CP $EXTRA_PROCESSOR_ARGS $PROJECT_DIR/src/my/example/Example1.java \
#   $PROJECT_DIR/src/my/example/Example2.java

# In case of using this script for Android projects, you must include paths to:
# android.jar, gen folder, all libs used, source code folder.
# The Android project must be built with "ant debug" before running this script.

set -u
set -e

function run_cf_javac() {
    processor=$1
    cp=$2:${CHECKERFRAMEWORK}/../annotation-tools/annotation-file-utilities/annotation-file-utilities.jar
    extra_args=$3
    java_files=$4
    # Runs CF's javac
    ${CHECKERFRAMEWORK}/checker/bin/javac -cp $cp -processor $processor -AuseJaifInference $extra_args $java_files
}

function read_input() {
    # This function separates extra arguments from java files received as arguments.
    processor=$1
    cp=$2
    extra_args=""
    java_files=""
    index=2
    for i in "${@:3}" # Ignores first two arguments (processor and cp).
    do
        let "index++"
        if [[ $i != -* ]]; then
            java_files="${@:index}"
            break
        fi
        extra_args="$extra_args $i"
    done
}

# Main
if [ "$#" -lt 3 ]; then
    echo "infer-and-annotate.sh: Expected at least 3 arguments. Received the following arguments: $@. Check the script's documentation."
    exit 1
fi

read_input $@

DIFF=$(diff -q prev-jaif build/jaif-files || echo "Files dont exist.")
while [ "$DIFF" != "" ]
do
    echo "once"
    rm -rf prev-jaif 
    cp -r build/jaif-files prev-jaif 2>/dev/null || :
    echo "asd"
    run_cf_javac $processor $cp $extra_args $java_files
    insert-annotations-to-source build/jaif-files/* $java_files
    DIFF=$(diff -q prev-jaif build/jaif-files || echo 'Files dont exist.')
    echo "end"
    echo $DIFF
done

