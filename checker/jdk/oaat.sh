#!/usr/bin/bash

# compile One (annotated JDK source file) At A Time

# Debugging
PRESERVE=1  # option to preserve intermediate files

# parameters derived from environment
# TOOLSJAR and CTSYM derived from JAVA_HOME, rest from CHECKERFRAMEWORK
PARENTDIR="`cd $CHECKERFRAMEWORK/.. && pwd`"   # base directory
WORKDIR="${CHECKERFRAMEWORK}/checker/jdk"   # working directory
AJDK="${PARENTDIR}/annotated-jdk8u-jdk"        # annotated JDK
SRCDIR="${AJDK}/src/share/classes"
BINDIR="${WORKDIR}/build"
BOOTDIR="${WORKDIR}/bootstrap"              # initial build w/o processors
TOOLSJAR="${JAVA_HOME}/lib/tools.jar"
CF_BIN="${CHECKERFRAMEWORK}/checker/build"
CF_DIST="${CHECKERFRAMEWORK}/checker/dist"
CF_JAR="${CF_DIST}/checker.jar"
CF_JAVAC="java -Xms128m -Xmx512m -jar ${CF_JAR} -Xbootclasspath/p:${BOOTDIR}"
CTSYM="${JAVA_HOME}/lib/ct.sym"
CP="${BINDIR}:${BOOTDIR}:${TOOLSJAR}:${CF_BIN}:${CF_JAR}"
JFLAGS="-XDignore.symbol.file=true -Xmaxerrs 20000 -Xmaxwarns 20000 \
        -source 8 -target 8 -encoding ascii -cp ${CP}"
PROCESSORS="interning,nullness,signature"
PFLAGS="-Anocheckjdk -Aignorejdkastub -AuseDefaultsForUncheckedCode=source -Awarns"

PID=$$      # script process id
BOOT=0      # 0 to skip building bootstrap class directory

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
    echo "success!"
    kill -HUP ${PID}
}

if [ ${BOOT} -ne 0 ] ; then
    rm -rf ${BOOTDIR} ${BINDIR}
    mkdir -p ${BOOTDIR} ${BINDIR}
fi
cd ${SRCDIR}

SRC="`find com/sun/jarsigner com/sun/security com/sun/tools/attach \
           java javax/management jdk sun \
        \( -name dc -o -name jconsole -o -name repo -o -name snmp \) -prune \
        -o -name '*\.java' -print`"
# AGENDA keeps track of source files remaining to be processed
AGENDA="$1"

if [ -z "${AGENDA}" ] ; then
    echo "no annotated source files" | tee -a ${WORKDIR}/LOG
    exit 1
fi

if [ ${BOOT} -ne 0 ] ; then
    echo "build bootstrap JDK" | tee ${WORKDIR}/LOG
    javac -g -d ${BOOTDIR} ${JFLAGS} ${SRC} | tee -a ${WORKDIR}/LOG
    [ $? -ne 0 ] && exit 1
    grep -q 'not found' ${WORKDIR}/LOG
    [ $? -eq 0 ] && exit 0
    (cd ${BOOTDIR} && jar cf ../jdk.jar *)
fi

echo "build individually w/processors on" | tee ${WORKDIR}/LOG
echo `echo ${AGENDA} | wc -w` files
for f in ${AGENDA} ; do
    ${CF_JAVAC} -g -d ${BINDIR} ${JFLAGS} -processor ${PROCESSORS} ${PFLAGS} \
            $f 2>&1 | tee ${WORKDIR}/LOG
done

AGENDA=`grep 'Compilation unit: ' ${WORKDIR}/LOG | awk '{print$3}' | sort -u`
if [ -z "${AGENDA}" ] ; then
    finish | tee -a ${WORKDIR}/LOG
    [ $? -eq 0 ] && exit 0
fi

echo "failed"
exit 1
