#!/bin/sh

# This script runs the Checker Framework's whole-program inference
# iteratively on a program, adding type annotations to the program, until the
# .jaif files from one iteration are the same as the .jaif files from the
# previous iteration (which means there is nothing new to infer anymore).

# To use this script, the $CHECKERFRAMEWORK variable must be set to the
# Checker Framework's directory. Also, the AFU's insert-annotations-to-source
# program must be on the search path (that is, in the PATH environment variable).

# This script receives as arguments:
# 0. Any number of cmd-line arguments to insert-annotations-to-source (optional).
# 1. Processor's name (in any form recognized by CF's javac -processor argument).
# 2. Classpath (target project's classpath).
# 3. Any number of extra processor arguments to be passed to the checker.
# 4. Any number of paths to .jaif files -- used as input (optional).
# 5. Any number of paths to .java files in a program.

# Example of usage:
# ./infer-and-annotate.sh "LockChecker,NullnessChecker" \
#     plume-util/build/libs/plume-util-all.jar \
#     `find plume-util/src/main/java/ -name "*.java"`

# In case of using this script for Android projects, the classpath must include
# paths to: android.jar, gen folder, all libs used, source code folder.
# The Android project must be built with "ant debug" before running this script.

# TODO: This script deletes all .unannotated files, including ones that could
# have been generated previously by another means other than using this script.
# We must decide if we want to make a backup of previously generated
# .unannotated files, or if we want to keep the first set of generated
# .unannotated files.

# Halts the script when a nonzero value is returned from a command.
set -e

# Path to directory that will contain .jaif files after running the CF
# with -Ainfer
WHOLE_PROGRAM_INFERENCE_DIR=build/whole-program-inference

# Path that will contain .class files after running CF's javac. This dir will
# be deleted after executing this script.
TEMP_DIR=build/temp-whole-program-inference-output

# Path to directory that will contain .jaif files from the previous iteration.
PREV_ITERATION_DIR=build/prev-whole-program-inference

debug=
interactive=
# For debugging
# debug=1
# Require user confirmation before running each command
# interactive=1

CHECKERBIN=$(dirname "$0")

# This function separates extra arguments passed to the checker from Java files
# received as arguments.
# TODO: Handle the following limitation: This function makes the assumption
# that every argument starts with a hyphen. It means one cannot pass arguments
# such as -processorpath and -source, which are followed by a value.
read_input() {

    # Collect command-line arguments that come before the preprocessor.
    # Assumes that every command line argument starts with a hyphen.
    insert_to_source_args=""
    for i in "$@"
    do
        case "$1" in
            -*)
                insert_to_source_args="$insert_to_source_args $1"
                shift
            ;;
            *)
                break
            ;;
        esac
    done

    # First two arguments are processor and cp.
    processor=$1
    cp=$2
    shift
    shift

    extra_args=""
    java_files=""
    jaif_files=""
    for i in "$@"
    do
        # This function makes the assumption that every extra argument
        # starts with a hyphen. The rest are .java/.jaif files.
        case "$1" in
            -*)
                extra_args="$extra_args $1"
            ;;
            *.jaif)
                jaif_files="$jaif_files $1"
            ;;
            *.java)
                java_files="$java_files $1"
            ;;
        esac
        shift
    done
}

# Iteratively runs the Checker
infer_and_annotate() {
    mkdir -p $TEMP_DIR
    DIFF_JAIF=firstdiff
    # Create/clean whole-program-inference directory.
    rm -rf $WHOLE_PROGRAM_INFERENCE_DIR
    mkdir -p $WHOLE_PROGRAM_INFERENCE_DIR
    # If there are .jaif files as input, copy them.
    for file in $jaif_files;
    do
        cp $file $WHOLE_PROGRAM_INFERENCE_DIR/
    done

    # Perform inference and add annotations from .jaif to .java files until
    # $PREV_ITERATION_DIR has the same contents as $WHOLE_PROGRAM_INFERENCE_DIR.
    while [ "$DIFF_JAIF" != "" ]
    do
        # Updates $PREV_ITERATION_DIR folder
        rm -rf $PREV_ITERATION_DIR
        mv $WHOLE_PROGRAM_INFERENCE_DIR $PREV_ITERATION_DIR
        mkdir -p $WHOLE_PROGRAM_INFERENCE_DIR

        # Runs CF's javac
        command="$CHECKERBIN/javac -d $TEMP_DIR/ -cp $cp -processor $processor -Ainfer -Awarns -Xmaxwarns 10000 $extra_args $java_files"
        echo "About to run: ${command}"
        if [ $interactive ]; then
            echo "Press any key to run command... "
            read _
        fi
        ${command} || true
        # Deletes .unannotated backup files. This is necessary otherwise the
        # insert-annotations-to-source tool will use this file instead of the
        # updated .java one.
        # See TODO about .unannotated file at the top of this file.
        for file in $java_files;
        do
            rm -f "${file}.unannotated"
        done
        if [ ! `find $WHOLE_PROGRAM_INFERENCE_DIR -prune -empty` ]
        then
            # Only insert annotations if there is at least one .jaif file.
            insert-annotations-to-source $insert_to_source_args -i `find $WHOLE_PROGRAM_INFERENCE_DIR -name "*.jaif"` $java_files
        fi
        # Updates DIFF_JAIF variable.
        # diff returns exit-value 1 when there are differences between files.
        # When this happens, this script halts due to the "set -e"
        # in its header. To avoid this problem, we add the "|| true" below.
        DIFF_JAIF="$(diff -qr $PREV_ITERATION_DIR $WHOLE_PROGRAM_INFERENCE_DIR || true)"
    done
    if [ ! $debug ]; then
        clean
    fi
}

clean() {
    # It might be good to keep the final .jaif files.
    # rm -rf $WHOLE_PROGRAM_INFERENCE_DIR
    rm -rf $PREV_ITERATION_DIR
    rm -rf $TEMP_DIR
    # See TODO about .unannotated file at the top of this file.
    for file in $java_files;
        do
            rm -f "${file}.unannotated"
    done
}

# Main
if [ "$#" -lt 3 ]; then
    echo "Aborting infer-and-annotate.sh: Expected at least 3 arguments, received $#."
    echo "Received the following arguments: $@"
    exit 1
fi

read_input "$@"
infer_and_annotate
