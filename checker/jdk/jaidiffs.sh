#!/bin/sh

# runs jaidiff.sh ("set difference" for JAIFs) on parallel directory trees
# input directories currently fixed as jdiff/a and jdiff/b, output as jdiff/d

# parameters derived from environment
# TOOLSJAR derived from JAVA_HOME, rest from CHECKERFRAMEWORK
JSR308="`cd $CHECKERFRAMEWORK/.. && pwd`"   # base directory
JAIDIFF=`cd \`dirname $0\` && pwd`/jaidiff.sh
AFU_HOME="$JSR308/annotation-tools"
AFU="${AFU_HOME}/annotation-file-utilities"
AFUJAR="${AFU_HOME}/annotation-file-utilities/annotation-file-utilities.jar"
TOOLSJAR="${JAVA_HOME}/lib/tools.jar"
LT_BIN="${JSR308}/jsr308-langtools/build/classes"
CF_BIN="${JSR308}/checker-framework/checker/build"
CP="${LT_BIN}:${TOOLSJAR}:${AFUJAR}:${CLASSPATH}:${CF_BIN}"

# find classfiles referring to checkerframework and thus presumably annotated
classfiles() {
    for f in `find * -name '*\.class' -print` ; do
        strings "$f" | grep -q checkerframework
        [ $? -eq 0 ] && echo "$f"
    done
}

# extracts annotations from classfiles in directory tree and stores them
# in correspondingly named JAIFs in same directory
extract() {
    (
    cd "$1" || return 1
    [ -z "*" ] && echo "empty input directory $1" && return 1
    find * -name '*\.jaif' -delete
    for f in `classfiles` ; do
        D="`dirname $f`"
        B="`basename $f .class`"
        CLASSPATH=${CP} ${AFU}/scripts/extract-annotations "$f"
        [ $? -ne 0 ] && echo "extract annotations failed on $f" && return 1
    done
    )
    return 0
}

mkdir -p jdiffs/d
extract jdiffs/a
[ $? -ne 0 ] && echo "a failed" && exit 1
extract jdiffs/b
[ $? -ne 0 ] && echo "b failed" && exit 1

# run jaidiff.sh on a and b trees, save results to d
cd jdiffs/a
for f in `find * -name '*\.jaif' -print` ; do
    if [ -r "../b/$f" ] ; then
        echo :$f:
        mkdir -p "../d/`dirname $f`"
        bash ${JAIDIFF} $f ../b/$f > ../d/$f && continue
        echo "d failed" && exit $?
    fi
done

