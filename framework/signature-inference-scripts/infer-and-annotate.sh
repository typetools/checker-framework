#!/bin/sh

# This script runs the Checker Framework's signature inference
# iteratively on a program, adding type annotations on the program, until the
# .jaif files from one iteration are the same as the .jaif files from the
# previous iteration.

# This script receives as arguments:
# 1. Processor's name.
# 2. Classpath (target project's classpath).
# 3. Extra processor arguments which will be passed to the checker.
# 4. List of paths to .java files in a program.

# Example of usage:
#./infer-and-annotate.sh org.checkerframework.common.value.ValueChecker \
#   $PROJECT_CP $EXTRA_PROCESSOR_ARGS $PROJECT_DIR/src/my/example/Example1.java \
#   $PROJECT_DIR/src/my/example/Example2.java

# In case of using this script for Android projects, you must include paths to:
# android.jar, gen folder, all libs used, source code folder.
# The Android project must be built with "ant debug" before running this script.

set -e

# This function separates extra arguments passed to the checker from java files
# received as arguments.
function read_input() {
    processor=$1
    cp=$2:${CHECKERFRAMEWORK}/../annotation-tools/annotation-file-utilities/annotation-file-utilities.jar
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

function infer_and_annotate() {
    DIFF=firstdiff
    rm -rf build/jaif-files
    # Perform inference and add annotations from .jaif to .java files until
    # prev-jaif has the same contents as build/jaif-files
    while [ "$DIFF" != "" ]
    do
        # Updates prev-jaif folder
        rm -rf prev-jaif
        cp -r build/jaif-files prev-jaif 2>/dev/null || :

        # Runs CF's javac
        ${CHECKERFRAMEWORK}/checker/bin/javac -cp $cp -processor $processor -AinferSignatures $extra_args $java_files

        # Deletes .unannotated backup files. This is necessary otherwise the
        # insert-annotations-to-source tool will use this file instead of the
        # updated .java one.
        for file in $java_files;
        do
            rm -f "${file}.unannotated"
        done

        # Updates DIFF variable
        DIFF=$(diff -q prev-jaif build/jaif-files || echo 'Files dont exist.')

        if [ "$DIFF" != "" ]; then
            # Inserts annotations from .jaif into .java files
            insert-annotations-to-source -i build/jaif-files/* $java_files
        fi
    done
}

# Main
if [ "$#" -lt 3 ]; then
    echo "infer-and-annotate.sh: Expected at least 3 arguments. Received the following arguments: $@. Check the script's documentation."
    exit 1
fi

read_input $@
infer_and_annotate

