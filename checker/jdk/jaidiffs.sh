#!/bin/sh

# jaidiff.sh for directory trees -- work in progress
# input directories currently fixed as jdiff/a and jdiff/b, output as jdiff/d

# parameters derived from environment
# TOOLSJAR derived from JAVA_HOME, rest from JSR308
AFU_HOME="$JSR308/annotation-tools"
AFU="${AFU_HOME}/annotation-file-utilities"
WORKDIR="${JSR308}/checker-framework/checker/jdk"
TOOLSJAR="${JAVA_HOME}/lib/tools.jar"
LT_BIN="${JSR308}/jsr308-langtools/build/classes"
CF_BIN="${JSR308}/checker-framework/checker/build"
CP="${LT_BIN}:${TOOLSJAR}:${CLASSPATH}:${CF_BIN}"
JAIDIFF=`cd \`dirname $0\` && pwd`/jaidiff.sh

# find classes referring to checkerframework and thus presumably annotated
# (Q: works as expected if annotations not fully qualified in source?)
# should calculate once and save...
classfiles() {
    for f in `find * -name '*\.class' -print` ; do
        strings "$f" | grep -q checkerframework
        [ $? -eq 0 ] && echo "$f"
    done
}

# extract-annotations on directory tree
extract() {
    cd "$1"
    [ -z "*" ] && echo "empty directory!" && return 1
    find * -name '*\.jaif' -delete
    for f in `classfiles` ; do
        D="`dirname $f`"
        B="`basename $f .class`"
        CLASSPATH=${CP} ${AFU}/scripts/extract-annotations "$f"
        [ $? -ne 0 ] && echo "$f" && return 1
        mv "$D/*.jaif" "$D/$B.jaif"
    done
    return 0
}

if [ -r "jdiffs/d" ] ; then
    extract jdiffs/a
    [ $? -ne 0 ] && echo "a failed" && exit 1
    extract jdiffs/b
    [ $? -ne 0 ] && echo "b failed" && exit 1
fi

# run jaidiff.sh on a and b trees, save results to d
cd jdiffs/a
for f in `find * -name '*\.jaif' -print` ; do
    mkdir -p "../d/`dirname $f`"
    bash "${JAIDIFF}" "$f" "../b/$f" > "../d/$f" && continue
    echo "d failed" && exit $?
done

