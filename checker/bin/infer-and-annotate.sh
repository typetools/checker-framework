#!/bin/sh

# This script runs the Checker Framework's signature inference
# iteratively on a program, adding type annotations to the program, until the
# .jaif files from one iteration are the same as the .jaif files from the
# previous iteration (which means there is nothing new to infer anymore).

# This script receives as arguments:
# 1. Processor's name (in any form recognized by javac's -processor argument).
# 2. Classpath (target project's classpath).
# 3. Extra processor arguments which will be passed to the checker.
# 4. List of paths to .java files in a program.

# Example of usage:
# ./infer-and-annotate.sh "LockChecker,NullnessChecker" \
#     $JSR308/plume-lib/java/plume.jar -AprintErrorStack \
#     `find $JSR308/plume-lib/java/src/plume/ -name "*.java"`

# In case of using this script for Android projects, the classpath must include
# paths to: android.jar, gen folder, all libs used, source code folder.
# The Android project must be built with "ant debug" before running this script.

# Halts the script when a nonzero value is returned from a command.
set -e

# Path to directory that will contain .jaif files after running the CF
# with -AinferSignatures
SIGNATURE_INFERENCE_DIR=build/signature-inference

# Path that will contain .class files after running CF's javac. This dir will
# deleted after executing this script.
TEMP_DIR=build/temp-signature-inference-output

# Path to directory will contain .jaif files from the previous interation.
PREV_ITERATION_DIR=build/prev-signature-inference

# Path to annotation-file-utilities.jar
AFU_JAR="${CHECKERFRAMEWORK}/../annotation-tools/annotation-file-utilities/annotation-file-utilities.jar"

# This function separates extra arguments passed to the checker from Java files
# received as arguments.
# TODO: Handle the following limitation: This function makes the assumption
# that every argument starts with a hyphen. It means one cannot pass arguments
# such as -processorpath and -source.
read_input() {
    processor=$1
    cp=$2:$AFU_JAR
    # Ignores first two arguments (processor and cp).
    shift
    shift
    extra_args=""
    java_files=""
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
    # Creates signature-inference directory if it doesn't exist already.
    # If it already exists, the existing .jaif files will be considered.
    # This allows the user to provide some .jaif files as input.
    mkdir -p $SIGNATURE_INFERENCE_DIR
    mkdir -p $TEMP_DIR
    # Perform inference and add annotations from .jaif to .java files until
    # $PREV_ITERATION_DIR has the same contents as $SIGNATURE_INFERENCE_DIR
    while [ "$DIFF_JAIF" != "" ]
    do
        # Updates $PREV_ITERATION_DIR folder
        rm -rf $PREV_ITERATION_DIR
        cp -r $SIGNATURE_INFERENCE_DIR $PREV_ITERATION_DIR

        # Runs CF's javac
        ${CHECKERFRAMEWORK}/checker/bin/javac -d $TEMP_DIR/ -cp $cp -processor $processor -AinferSignatures $extra_args $java_files || true
        # Deletes .unannotated backup files. This is necessary otherwise the
        # insert-annotations-to-source tool will use this file instead of the
        # updated .java one.
        for file in $java_files;
        do
            rm -f "${file}.unannotated"
        done
        if [ ! `find $SIGNATURE_INFERENCE_DIR -prune -empty` ]
        then
            # Only insert annotations if there is at least one .jaif file.
            insert-annotations-to-source -i $SIGNATURE_INFERENCE_DIR/* $java_files
        fi
        # Updates DIFF_JAIF variable.
        # diff returns exit-value 1 when there are differences between files.
        # When this happens, this script halts due to the "set -e"
        # in its header. To avoid this problem, we add the "|| true" below.
        DIFF_JAIF="$(diff -qr $PREV_ITERATION_DIR $SIGNATURE_INFERENCE_DIR || true)"
    done
    clean
}

clean() {
    # Do we want to delete possible .jaif files passed as input?
    # If so, uncomment line below:
    # rm -rf $SIGNATURE_INFERENCE_DIR
    rm -rf $PREV_ITERATION_DIR
    rm -rf $TEMP_DIR
    for file in $java_files;
        do
            rm -f "${file}.unannotated"
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

