#!/bin/sh

JSR308="`cd $CHECKERFRAMEWORK/.. && pwd`"
JDKDIR="${JSR308}/annotated-jdk8u-jdk"
CHKDIR="${CHECKERFRAMEWORK}/checker/jdk"
SRCDIR="${JDKDIR}/src/share/classes"
BINDIR="${CHKDIR}/build"
BOOTDIR="${CHKDIR}/bootstrap"
DISTDIR="${CHECKERFRAMEWORK}/checker/dist"
TOOLDIR="${JSR308}/jsr308-langtools/build/classes"
TOOLJAR="${JAVA_HOME}/lib/tools.jar"
PROCS="interning,igj,javari,nullness,signature"
JAVAC="java -jar ${DISTDIR}/checker.jar"
BOOTC="${JSR308}/jsr308-langtools/dist/bin/javac"
JFLAGS="-source 8 -target 8 -encoding ascii \
        -cp "${BINDIR}:${BOOTDIR}:${TOOLDIR}:${TOOLJAR}:${CHECKERFRAMEWORK}/checker/build:${CHECKERFRAMEWORK}/checker/dist/checker.jar" \
        -XDignore.symbol.file=true -Xmaxerrs 20000 -Xmaxwarns 20000"
PFLAGS="-Aignorejdkastub -AuseDefaultsForUncheckedCode=source -AprintErrorStack -Awarns"
CT="${JAVA_HOME}/lib/ct.sym"
RET=0

finish() {
    #extract annotations from classfiles and insert them into ct.sym
    echo "building JAR"
    mkdir -p ${CHKDIR}/sym
    rsync -a ${BINDIR}/* ${BOOTDIR}
    cd ${CHKDIR}/sym
    jar xf ${CT}
    cd ${CHKDIR}/sym/META-INF/sym/rt.jar
    for f in `find * -name '*\.class' -print` ; do
        B=`basename $f .class`
        D=`dirname $f`
        JAIF="$D/$B.jaif"
        if [ -f ${BOOTDIR}/$f ] ; then
            extract-annotations ${BOOTDIR}/$f
            insert-annotations $f ${BOOTDIR}/${JAIF}
            mkdir -p ${CHKDIR}/jaifs/$D
            mv ${BOOTDIR}/${JAIF} ${CHKDIR}/jaifs/$D
        fi
    done
    jar cf ${CHKDIR}/ct.sym *
    cd ${CHKDIR}
    rm -rf sym
    exit 0
}

checkfordone() {
    #if no files remain to be compiled, call finish
    if [ ${RET} -eq 0 ] && [ `echo ${ANN} | wc -w` -eq 0 ] ; then
        echo "succeeded!" && finish
    fi
}

cd ${SRCDIR}
rm -rf ${BOOTDIR} ${BINDIR}
mkdir -p ${BOOTDIR} ${BINDIR}

SRC="`find com/sun/jarsigner com/sun/security com/sun/tools/attach \
           java javax/management jdk sun \
        \( -name dc -o -name jconsole -o -name snmp \) -prune \
        -o -name '*\.java' -print`"
ANN=`grep -l -w checkerframework ${SRC}`

if [ `echo ${ANN} | wc -w` -eq 0 ] ; then
    echo "no annotated source files" | tee -a ${CHKDIR}/LOG0
    exit 1
fi

#warn of any files with checkerframework but not @AnnotatedFor
NAF=`grep -L -w '@AnnotatedFor' ${ANN}`
if [ `echo ${NAF} | wc -w` -ne 0 ] ; then
    echo "Warning: missing @AnnotatedFor:"
    for f in ${NAF} ; do
        echo "    $f"
    done
fi

echo "phase 0 (bootstrap)" | tee ${CHKDIR}/LOG0
(${BOOTC} -g -d ${BOOTDIR} ${JFLAGS} ${SRC} || exit $?) | tee -a ${CHKDIR}/LOG0

echo "phase 1" | tee ${CHKDIR}/LOG1
if [ ! -r ${DISTDIR} ] ; then
    echo making directory ${DISTDIR}
    mkdir -p ${DISTDIR}
fi
if [ ! -r ${DISTDIR}/javac.jar ] ; then
    echo copying javac JAR
    cp ${JSR308}/jsr308-langtools/dist/lib/javac.jar ${DISTDIR}
fi
if [ ! -r ${DISTDIR}/jdk8.jar ] ; then
    echo creating bootstrap JDK 8 JAR
    (cd ${BOOTDIR} && jar cf ${CHKDIR}/jdk8.jar * && \
             cp ${CHKDIR}/jdk8.jar ${DISTDIR}) || exit $?
fi

(${JAVAC} -g -d ${BINDIR} ${JFLAGS} -processor ${PROCS} ${PFLAGS} ${ANN} 2>&1 \
        || RET=$?) | tee -a ${CHKDIR}/LOG1
[ ${RET} -eq 0 ] || exit ${RET}

#hack: scrape log file to find which source files crashed
#TODO: check for corresponding class files instead
ANN=`grep 'Compilation unit: ' ${CHKDIR}/LOG1 | awk '{print$3}' | sort -u`
checkfordone | tee -a ${CHKDIR}/LOG1

#retry (annotated) failures w/ processors on
echo "phase 2" | tee ${CHKDIR}/LOG2
(${JAVAC} -g -d ${BINDIR} ${JFLAGS} -processor ${PROCS} ${PFLAGS} \
 ${ANN} 2>&1 || RET=$?) | tee -a ${CHKDIR}/LOG2

ANN=`grep 'Compilation unit: ' ${CHKDIR}/LOG2 | awk '{print$3}' | sort -u`
checkfordone | tee -a ${CHKDIR}/LOG2

#retry any remaining failures individually w/ processors on
echo "phase 3" | tee ${CHKDIR}/LOG3
for f in $ANN ; do
    (${JAVAC} -g -d ${BINDIR} ${JFLAGS} -processor ${PROCS} ${PFLAGS} \
     $f 2>&1 | tee -a ${CHKDIR}/LOG3)
done

ANN=`grep 'Compilation unit: ' ${CHKDIR}/LOG3 | awk '{print$3}' | sort -u`
checkfordone | tee -a ${CHKDIR}/LOG3

#retry remaining failures individually w/ individual processors on;
#extract annotations from resulting class files;
#compile w/o processors and then re-insert all annotations
echo "phase 4" | tee ${CHKDIR}/LOG4
for f in $ANN ; do
    J=`dirname $f`/`basename $f .java`
    JAIF=`echo $J | tr . /`.jaif

    #extract
    mkdir -p jaifs
    for p in `echo ${PROCS} | tr , '\012'` ; do
        (${JAVAC} -g -d ${BINDIR} ${JFLAGS} -processor $p ${PFLAGS} \
         $f 2>&1 || RET=$?) | tee -a ${CHKDIR}/LOG4
        if [ -r $J.class ] ; then
            (extract-annotations $J.class 2>&1 || RET=$?) | \
                    tee -a ${CHKDIR}/LOG4
            if [ $? -eq 0 ] ; then
                mkdir -p jaifs/$p
                mv ${JAIF} jaifs/$p
            fi
        fi
    done

    #recompile/insert
    (${JAVAC} -g -d ${BINDIR} ${JFLAGS} $f 2>&1 || RET=$?) | \
            tee -a ${CHKDIR}/LOG4
    if [ -r $J.class ] ; then
        for p in `echo ${PROCS} | tr , '\012'` ; do
            if [ -r jaifs/p/${JAIF} ] ; then
                (insert-annotations $J.class jaifs/p/${JAIF} || RET=1) | \
                        tee -a ${CHKDIR}/LOG4
            fi
        done
    fi
done
checkfordone | tee -a ${CHKDIR}/LOG4

