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
# ./infer-and-annotate.sh "LockChecker,NullnessChecker" \
#     $JSR308/plume-lib/java/plume.jar -AprintErrorStack \
#     `find $JSR308/plume-lib/java/src/plume/ -name "*.java"`

# In case of using this script for Android projects, you must include paths to:
# android.jar, gen folder, all libs used, source code folder.
# The Android project must be built with "ant debug" before running this script.

# Halts the script when a nonzero value is returned from a command.
set -e

# This function separates extra arguments passed to the checker from java files
# received as arguments.
# TODO: Handle the following limitation: This function makes the assumption
# that every argument starts with a hyphen. It means one cannot pass arguments
# such as -processorpath and -source.
read_input() {
    processor=$1
    cp=$2:${CHECKERFRAMEWORK}/../annotation-tools/annotation-file-utilities/annotation-file-utilities.jar
    extra_args=""
    java_files=""

    # Ignores first two arguments (processor and cp).
    shift
    shift

    for i in "$@"
    do
        # This function makes the assumption that every extra argument
        # starts with a hyphen. The rest are .java files.
        case "$1" in
            -*)
                extra_args="$extra_args $1"
            ;;
            *)
                java_files="$java_files $1"
            ;;
        esac
        shift
    done
}

# Iteratively runs the Checker
infer_and_annotate() {
    DIFF_JAIF=firstdiff
    rm -rf build/jaif-files
    mkdir -p build/jaif-files
    # Perform inference and add annotations from .jaif to .java files until
    # prev-jaif has the same contents as build/jaif-files
    while [ "$DIFF_JAIF" != "" ]
    do
        # Updates prev-jaif folder
        rm -rf prev-jaif
        cp -r build/jaif-files prev-jaif

        # Runs CF's javac
        ${CHECKERFRAMEWORK}/checker/bin/javac -cp $cp -processor $processor -AinferSignatures $extra_args $java_files
        # Deletes .unannotated backup files. This is necessary otherwise the
        # insert-annotations-to-source tool will use this file instead of the
        # updated .java one.
        for file in $java_files;
        do
            rm -f "${file}.unannotated"
        done
        insert-annotations-to-source -i build/jaif-files/* $java_files
        # Updates DIFF_JAIF variable.
        # diff returns exit-value 1 when there are differences between files.
        # When this happens, this script halts due to the "set -e"
        # in its header. To avoid this problem, we add the "|| true" below.
        DIFF_JAIF="$(diff -qr prev-jaif build/jaif-files || true)"
    done
}

# Main
if [ "$#" -lt 3 ]; then
    echo "Aborting infer-and-annotate.sh: Expected at least 3 arguments. Check the script's documentation."
    echo "Received the following arguments: $@."
    exit 1
fi

read_input "$@"
infer_and_annotate

