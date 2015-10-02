#!/bin/bash

# This script receives as arguments a path to a directory containing .jaif files
# and a path to a directory containing .java files. It runs the
# insert-annotations-to-source tool for each file in the directories passed as
# arguments. It assumes that both sets are in a similar file
# hierarchy. For example, the $file jaif_path/package/foo.jaif will have its
# annotations written into $java_source_path/package/foo.java.

function insert_annos_from_jaif() {
    source_files_dir=$1
    jaif_files_dir=$2
    root_dir=`pwd`
    cd $source_files_dir
    for f in $(find . -name "*.java"); do
        f=${f:2}
        jaif_file_path=$jaif_files_dir$f
        jaif_file_path="${jaif_file_path:0:${#jaif_file_path}-2}if"
        cd $root_dir
        insert-annotations-to-source --outdir=$source_files_dir/../annotated/ $jaif_file_path $source_files_dir/$f
        cd $source_files_dir
    done

}

# Main
if [ "$#" -lt 2 ]; then
    echo "Illegal number of parameters. Check the script's documentation."
    exit 1
fi

source_files_dir=$1
jaif_files_dir=$2

insert_annos_from_jaif $source_files_dir $jaif_files_dir

