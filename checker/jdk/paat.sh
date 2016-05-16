#!/bin/sh

# compile Package At A Time

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
CF_JAVAC="java -Xms128m -Xmx512m -jar ${CF_JAR} -Xbootclasspath/p:${BOOTDIR}"
CTSYM="${JAVA_HOME}/lib/ct.sym"
CP="${BINDIR}:${BOOTDIR}:${LT_BIN}:${TOOLSJAR}:${CF_BIN}:${CF_JAR}"
JFLAGS="-XDignore.symbol.file=true -Xmaxerrs 20000 -Xmaxwarns 20000 \
        -source 8 -target 8 -encoding ascii -cp ${CP}"
PROCESSORS="interning,igj,javari,nullness,signature"
PFLAGS="-Anocheckjdk -Aignorejdkastub -AuseDefaultsForUncheckedCode=source -AprintErrorStack -Awarns"

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
    jar cf ${WORKDIR}/jdk.jar *
    cd ${WORKDIR}
    [ ${PRESERVE} -ne 0 ] || rm -rf sym
    return 0
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
DIRS=`(cd ${WORKDIR} && cat DIRS)`
# AGENDA keeps track of source files remaining to be processed
AGENDA="$SRC"

if [ -z "${DIRS}" ] ; then
    echo "no annotated source files"
    exit 1
fi

if [ ${BOOT} -ne 0 ] ; then
    echo "build bootstrap JDK"
    ${LT_JAVAC} -g -d ${BOOTDIR} ${JFLAGS} ${SRC} | tee ${WORKDIR}/LOG
    [ $? -ne 0 ] && exit 1
    grep -q 'not found' ${WORKDIR}/LOG
    [ $? -eq 0 ] && exit 0
    (cd ${BOOTDIR} && jar cf ../jdk.jar *)
fi

echo "build one package at a time w/processors on"
rm -rf ${WORKDIR}/log
mkdir -p ${WORKDIR}/log
for d in ${DIRS} ; do
    echo :$d: `echo $d/*.java | wc -w` files
    ${CF_JAVAC} -g -d ${BINDIR} ${JFLAGS} -processor ${PROCESSORS} ${PFLAGS} \
            "$d"/*.java 2>&1 | tee ${WORKDIR}/log/`echo "$d" | tr / .`.log
done

AGENDA=`cat ${WORKDIR}/log/* | grep -l 'Compilation unit: ' | awk '{print$3}' | sort -u`
if [ -z "${AGENDA}" ] ; then
    finish | tee -a ${WORKDIR}/LOG
    [ $? -eq 0 ] && exit 0
fi

echo "failed"
exit 1

