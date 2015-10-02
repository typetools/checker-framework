#!/bin/bash

# This script receives as arguments:
# 1. Processor name.
# 2. Classpath (target project's classpath and annotation-file-utilities.jar path)
# 3. Project source folder path
# 4. Extra processor arguments

# Example of usage:
#./infer-and-annotate.sh org.checkerframework.common.value.ValueChecker $PROJECT_CP:..../annotation-file-utilities.jar project_src_folder/ $EXTRA_PROCESSOR_ARGS
# $PROJECT_CP must contain a path to annotation-file-utilities.jar. In case of 
# using this script for Android projects, you must include paths to:
# android.jar, gen folder, all libs used, source code folder.
# The Android project must be built with "ant debug" before running this script.

function run_cf_javac() {
    processor=$1
    cp=$2
    source_files_dir=$3
    extra_args=$4
    cp=$cp:${CHECKERFRAMEWORK}/../annotation-tools/annotation-file-utilities/annotation-file-utilities.jar
    files_to_compile=""
    for f in $(find $source_files_dir/ -name "*.java"); do
        files_to_compile="$files_to_compile $f"
    done
    # Runs CF's javac
    ${CHECKERFRAMEWORK}/checker/bin/javac -cp $cp -processor $processor -AuseJaifInference $extra_args $files_to_compile
}

# Main
if [ "$#" -lt 3 ]; then
    echo "Illegal number of parameters. Check the script's documentation."
    exit 1
fi

processor=$1
cp=$2
source_files_dir=$3
extra_args=$4



run_cf_javac $processor $cp $source_files_dir $extra_args
cur_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
$cur_dir/insert-annotations.sh $source_files_dir build/jaif-files/

