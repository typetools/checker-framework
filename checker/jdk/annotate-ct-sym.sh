#!/bin/sh

# Builds JDK jar for Checker Framework by inserting annotations from
# annotated JDK class files into ct.sym.

# ensure CHECKERFRAMEWORK set
if [ -z "$CHECKERFRAMEWORK" ] ; then
    if [ -z "$CHECKER_FRAMEWORK" ] ; then
        export CHECKERFRAMEWORK=`(cd "$0/../.." && pwd)`
    else
        export CHECKERFRAMEWORK=${CHECKER_FRAMEWORK}
    fi
fi
[ $? -eq 0 ] || (echo "CHECKERFRAMEWORK not set; exiting" && exit 1)

# Debugging
PRESERVE=1  # option to preserve intermediate files

# parameters derived from environment
# TOOLSJAR and CTSYM derived from JAVA_HOME, rest from CHECKERFRAMEWORK
JSR308="`cd $CHECKERFRAMEWORK/.. && pwd`"   # base directory
WORKDIR="${CHECKERFRAMEWORK}/checker/jdk"   # working directory
AJDK="${HOME}/sandbox/bjdk/jdk"             # annotated JDK
#AJDK="${JSR308}/annotated-jdk8u-jdk"        # annotated JDK
SRCDIR="${AJDK}/src/share/classes"
BINDIR="${WORKDIR}/build"  # TODO: make into parameter, or depend on 7/8
LT_BIN="${JSR308}/jsr308-langtools/build/classes"
LT_JAR="${JAVA_HOME}/lib/tools.jar"
CF_BIN="${CHECKERFRAMEWORK}/checker/build"
CF_DIST="${CHECKERFRAMEWORK}/checker/dist"
CF_JAR="${CF_DIST}/checker.jar"
CP="${BINDIR}:${LT_BIN}:${LT_JAR}:${CF_BIN}:${CF_JAR}"
CTSYM="${JAVA_HOME}/lib/ct.sym"

# TODO: incorporate this logic into checker/build.xml?
## if present, JAVA_7_HOME overrides JAVA_HOME
#[ -z "${JAVA_7_HOME}" ] || CTSYM="${JAVA_7_HOME}/lib/ct.sym"

set -o pipefail

# This is called only when all source files successfully compiled.
# It does the following:
#  * explodes ct.sym
#  * for each annotated classfile:
#     * extracts its annotations
#     * inserts the annotations into the classfile's counterpart
#       in the ct.sym class directory
#  * repackages the resulting classfiles as jdkX.jar.

rm -rf ${WORKDIR}/sym ${WORKDIR}/jaifs
mkdir -p ${WORKDIR}/sym
cd ${WORKDIR}/sym
echo "building JAR"

# unjar ct.sym
jar xf ${CTSYM}
#cd ${WORKDIR}/sym/META-INF/sym/rt.jar  # yes, it's a directory
(cd ${WORKDIR}/sym/META-INF/sym/rt.jar && rsync -a ${BINDIR}/* .)
#(cd ${WORKDIR}/sym/META-INF/sym/rt.jar && rm -rf * && jar xf ${CHECKERFRAMEWORK}/jdk8.jar)

# TODO: implement Java 7 logic, described below but never implemented
# Explode (Java 7) ct.sym, extract annotations from jdk8.jar, insert
# extracted annotations into ct.sym classfiles, and repackage newly
# annotated classfiles as jdk7.jar.

# annotate class files
for f in `find * -name '*\.class' -print` ; do
    B=`basename $f .class`
    D=`dirname $f`
    if [ -r ${BINDIR}/$f ] ; then
        echo "extract-annotations ${BINDIR}/$f"
        CLASSPATH=${CP} extract-annotations ${BINDIR}/$f
        JAIFS=`echo ${BINDIR}/$D/*.jaif`
        for g in ${JAIFS} ; do
            CLS="$D/`basename $g .jaif`.class"
            if [ -r "${CLS}" ] ; then
                echo "insert-annotations $CLS $g"
                insert-annotations "$CLS" "$g"
            else
                echo ${CLS}: not found
            fi
        done

        if [ ${PRESERVE} -ne 0 ] ; then
            # save JAIFs for analysis
            DEST=${WORKDIR}/jaifs/$D
            mkdir -p ${DEST} && mv ${JAIFS} ${DEST}
        fi
    fi
done

# recreate jar
rm -f ${WORKDIR}/jdk.jar
jar cf ${WORKDIR}/jdk.jar *

