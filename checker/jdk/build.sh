#!/usr/bin/bash

# Builds jdk8.jar for Checker Framework, using multiple compilation
# phases to work around processor bugs.
#
# If a phase successfully processes all files, it calls the finish function
# to create jdk8.jar from ct.sym, and the script exits with status 0.
# Otherwise, the next phase processes only the subset of files for which
# processing has so far failed.

# Phase 0: an initial bootstrap build with processors off
#
# Phase 1 tries to process all annotated source files together.
#
# Phase 2 does the same as Phase 1 with the remaining files, in case the
# annotated class files from the previous phase somehow provide additional
# information that allows the processors to succeed.
#
# Phase 3 processes each remaining file individually.
#
# Phase 4 does so as well, but running only one processor at a time,
# merging annotations at the end using Annotation File Utilities.


# Debugging
PRESERVE=1  # option to preserve intermediate files

# parameters derived from environment
# TOOLSJAR and CTSYM derived from JAVA_HOME, rest from CHECKERFRAMEWORK
JSR308="`cd $CHECKERFRAMEWORK/.. && pwd`"   # base directory
WORKDIR="${CHECKERFRAMEWORK}/checker/jdk"   # working directory
AJDK="${JSR308}/annotated-jdk8u-jdk"        # annotated JDK
SRCDIR="${AJDK}/src/share/classes"
BINDIR="${WORKDIR}/build"
BOOTDIR="${WORKDIR}/bootstrap"              # initial build w/o processors
TOOLSJAR="${JAVA_HOME}/lib/tools.jar"
LT_BIN="${JSR308}/jsr308-langtools/build/classes"
LT_JAVAC="${JSR308}/jsr308-langtools/dist/bin/javac"
CF_BIN="${CHECKERFRAMEWORK}/checker/build"
CF_DIST="${CHECKERFRAMEWORK}/checker/dist"
CF_JAR="${CF_DIST}/checker.jar"
CF_JAVAC="java -jar ${CF_JAR} -Xbootclasspath/p:${BOOTDIR}"
CTSYM="${JAVA_HOME}/lib/ct.sym"
CP="${BINDIR}:${BOOTDIR}:${LT_BIN}:${TOOLSJAR}:${CF_BIN}:${CF_JAR}"
JFLAGS="-XDignore.symbol.file=true -Xmaxerrs 20000 -Xmaxwarns 20000 \
        -source 8 -target 8 -encoding ascii -cp ${CP}"
PROCESSORS="interning,igj,javari,nullness,signature"
PFLAGS="-Aignorejdkastub -AuseDefaultsForUncheckedCode=source -AprintErrorStack -Awarns"

RET=0       # exit code initialization
PID=$$      # script process id

trap "exit 0" SIGHUP
set -o pipefail

# This is called only when all source files successfully compiled.
# It does the following:
#  * explodes ct.sym
#  * for each annotated classfile:
#     * extracts its annotations
#     * inserts the annotations into the classfile's counterpart
#       in the ct.sym class directory
#  * repackages the resulting classfiles as jdk8.jar.
finish() {
    echo "building JAR"
    rm -rf ${WORKDIR}/sym
    mkdir -p ${WORKDIR}/sym
    cd ${WORKDIR}/sym
    # unjar ct.sym
    jar xf ${CTSYM}
    cd ${WORKDIR}/sym/META-INF/sym/rt.jar  # yes, it's a directory
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
                mkdir -p ${DEST}
                mv ${JAIFS} ${DEST}
            fi
        fi
    done
    # recreate jar
    jar cf ${WORKDIR}/jdk8.jar *
    cd ${WORKDIR}
    cp jdk8.jar ${CF_DIST}
    [ ${PRESERVE} -ne 0 ] || rm -rf sym
    echo "success!"
    kill -HUP ${PID}
}

cd ${SRCDIR}
rm -rf ${BOOTDIR} ${BINDIR}
mkdir -p ${BOOTDIR} ${BINDIR}

SRC="`find com/sun/jarsigner com/sun/security com/sun/tools/attach \
           java javax/management jdk sun \
        \( -name dc -o -name jconsole -o -name snmp \) -prune \
        -o -name '*\.java' -print`"
# AGENDA keeps track of source files remaining to be processed
AGENDA=`grep -l -w checkerframework ${SRC}`

if [ -z "${AGENDA}" ] ; then
    echo "no annotated source files" | tee -a ${WORKDIR}/LOG0
    exit 1
fi

# warn of any files containing "checkerframework" but not "@AnnotatedFor"
NAF=`grep -L -w '@AnnotatedFor' ${AGENDA}`
if [ ! -z "${NAF}" ] ; then
    echo "Warning: missing @AnnotatedFor:"
    for f in ${NAF} ; do
        echo "    $f"
    done
fi

echo "phase 0: build bootstrap JDK" | tee ${WORKDIR}/LOG0
echo "${LT_JAVAC} -g -d ${BOOTDIR} ${JFLAGS} [$(pwd)]" | tee -a ${WORKDIR}/LOG0
${LT_JAVAC} -g -d ${BOOTDIR} ${JFLAGS} ${SRC} | tee -a ${WORKDIR}/LOG0
RET=$?
[ ${RET} -eq 0 ] || exit ${RET}
grep -q 'not found' ${WORKDIR}/LOG0
RET=$?
[ ${RET} -ne 0 ] || exit ${RET}

echo "phase 1: process all source files together" | tee ${WORKDIR}/LOG1
# The first command could be replaced by "cd ${CHECKERFRAMEWORK} && ant
# dist-nobuildjdk", but that would take longer and produce much more output.
[ ! -r ${CF_DIST}/javac.jar ] && echo copying javac JAR && \
        mkdir -p ${CF_DIST} && \
        cp ${JSR308}/jsr308-langtools/dist/lib/javac.jar ${CF_DIST}
[ ! -r ${CF_DIST}/jdk8.jar ] && echo creating bootstrap JDK 8 JAR && \
        cd ${BOOTDIR} && jar cf ${WORKDIR}/jdk8.jar * && \
        cp ${WORKDIR}/jdk8.jar ${CF_DIST}

${CF_JAVAC} -g -d ${BINDIR} ${JFLAGS} -processor ${PROCESSORS} ${PFLAGS} \
        ${AGENDA} 2>&1 | tee -a ${WORKDIR}/LOG1

# hack: scrape log file to find which source files crashed
# TODO: check for corresponding class files instead
AGENDA=`grep 'Compilation unit: ' ${WORKDIR}/LOG1 | awk '{print$3}' | sort -u`
[ -z "${AGENDA}" ] && finish | tee -a ${WORKDIR}/LOG1

# retry failures with all phase 1 class files available in the classpath
echo "phase 2: retry failures" | tee ${WORKDIR}/LOG2
${CF_JAVAC} -g -d ${BINDIR} ${JFLAGS} -processor ${PROCESSORS} ${PFLAGS} \
         ${AGENDA} 2>&1 | tee -a ${WORKDIR}/LOG2

AGENDA=`grep 'Compilation unit: ' ${WORKDIR}/LOG2 | awk '{print$3}' | sort -u`
[ -z "${AGENDA}" ] && finish | tee -a ${WORKDIR}/LOG2

# retry remaining failures individually with all processors on
echo "phase 3: retry failures individually" | tee ${WORKDIR}/LOG3
for f in ${AGENDA} ; do
    ${CF_JAVAC} -g -d ${BINDIR} ${JFLAGS} -processor ${PROCESSORS} ${PFLAGS} \
            $f 2>&1 | tee -a ${WORKDIR}/LOG3
done

AGENDA=`grep 'Compilation unit: ' ${WORKDIR}/LOG3 | awk '{print$3}' | sort -u`
[ -z "${AGENDA}" ] && finish | tee -a ${WORKDIR}/LOG3

# retry remaining failures individually with each processor, one at a time;
# extract annotations from resulting class files;
# compile w/o processors and then re-insert all annotations
echo "phase 4: retry failures individually with one processor at a time" \
        | tee ${WORKDIR}/LOG4
mkdir -p jaifs
RET=0

for f in ${AGENDA} ; do
    BASE="`dirname $f`/`basename $f .java`"
    CLS="${BASE}.class"

    # extract annotations
    for p in `echo ${PROCESSORS} | tr , '\012'` ; do
        ${CF_JAVAC} -g -d ${BINDIR} ${JFLAGS} -processor $p ${PFLAGS} \
                 $f 2>&1 | tee -a ${WORKDIR}/LOG4
        if [ $? -eq 0 -a -r ${CLS} ] ; then
            for c in "${BASE}.class ${BASE}\$*.class" ; do
                echo extracting from: $c
                CLASSPATH=${CP} extract-annotations "$c" 2>&1 | \
                        tee -a ${WORKDIR}/LOG4
                S=$?
                [ ${RET} -eq 0 ] && RET=$S
            done
            JAIFS="${BINDIR}/$D/*.jaif"
            if [ -z "${JAIFS}" ] ; then
                mkdir -p jaifs/$p
                mv ${JAIFS} jaifs/$p
            fi
        fi
    done
    [ ${RET} -ne 0 ] && echo "$f: extraction failed" && exit ${RET}

    # insert all annotations into unannotated class files
    ${CF_JAVAC} -g -d ${BINDIR} ${JFLAGS} $f 2>&1 | tee -a ${WORKDIR}/LOG4
    RET=$?
    if [ ${RET} -eq 0 -a -r ${CLS} ] ; then
        for g in jaifs/*/*.jaif ; do
            echo inserting into: $c
            insert-annotations "${CLS}" "$g" | tee -a ${WORKDIR}/LOG4
            RET=$?
            rm -f "$g"
        done
    fi
    [ ${RET} -ne 0 ] && echo "${CLS}: insertion failed" && exit ${RET}
done

[ ${PRESERVE} -eq 0 ] && rm -rf jaifs
finish | tee -a ${WORKDIR}/LOG4

