#!/usr/bin/bash

# Builds JDK jar for Checker Framework by inserting annotations into
# ct.sym.

# Debugging
PRESERVE=1  # option to preserve intermediate files

# parameters derived from environment
# CTSYM derived from JAVA_HOME, rest from CHECKERFRAMEWORK
DIST="${CHECKERFRAMEWORK}/checker/dist"
CHECKERJAR="${DIST}/checker.jar"
TOOLJAR="${CHECKERFRAMEWORK}/../jsr308-langtools/dist/lib/javac.jar"
WORKDIR="${CHECKERFRAMEWORK}/checker/jdk"
BINDIR="${WORKDIR}/build"
JAIFDIR="${WORKDIR}/jaifs"
SYMDIR="${WORKDIR}/sym"
CP="${BINDIR}:${CHECKERJAR}:${TOOLJAR}"
CTSYM="${JAVA_HOME}/lib/ct.sym"
# if present, JAVA_7_HOME overrides JAVA_HOME
[ -z "${JAVA_7_HOME}" ] || CTSYM="${JAVA_7_HOME}/lib/ct.sym"

set -o pipefail

# Explode (Java 7) ct.sym, extract annotations from jdk8.jar, insert
# extracted annotations into ct.sym classfiles, and repackage newly
# annotated classfiles as jdk7.jar.

rm -rf ${SYMDIR}
mkdir -p ${SYMDIR}
cd ${SYMDIR}

# unjar ct.sym
jar xf ${CTSYM}
cd ${WORKDIR}/sym/META-INF/sym/rt.jar  # yes, it's a directory

# annotate class files
rm -rf ${JAIFDIR}
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

        mkdir -p ${JAIFDIR}/$D
        mv ${JAIFS} ${JAIFDIR}/$D
    fi
done

# recreate jar
jar cf ${WORKDIR}/jdk7.jar *
cd ${WORKDIR}
cp jdk7.jar ${DIST}
[ ${PRESERVE} -ne 0 ] || rm -rf ${JAIFDIR} ${SYMDIR}

