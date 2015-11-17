#!/bin/sh

# This script insert annotations in a set of .java files given a set of .jaif
# files.

# This script receives as arguments a path to a directory containing .jaif files
# and a path to a directory containing .java files. It runs the
# insert-annotations-to-source tool for each file in the directories passed as
# arguments. It assumes that both sets are in a similar file
# hierarchy. For example, the $file jaif_path/package/foo.jaif will have its
# annotations written into $java_source_path/package/foo.java.

set -u
set -e

function insert_annos_from_jaif() {
    source_files_dir=$1
    jaif_files_dir=$2
    root_dir=`pwd`
    cd $source_files_dir
    for f in $(find . -name "*.java"); do
        # Stripping leading "./"
        f=${f:2}
        jaif_file_path=$jaif_files_dir$f
        # Converting ".java" to ".jaif"
        jaif_file_path="${jaif_file_path:0:${#jaif_file_path}-2}if"

        cd $root_dir
        # Runs insert-annotations-to-source for a single file
        insert-annotations-to-source --outdir=$source_files_dir/../annotated/ $jaif_file_path $source_files_dir/$f
        cd $source_files_dir
    done

}

# Main
if [ "$#" -lt 2 ]; then
    echo "insert-annotation.sh: Expected at least 2 arguments. Received the following arguments: $@. Check the script's documentation."
    exit 1
fi

source_files_dir=$1
jaif_files_dir=$2

insert_annos_from_jaif $source_files_dir $jaif_files_dir

