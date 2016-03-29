#!/bin/sh

# strip Checker Framework annotations out of source files

INDIR=$JSR308/annotated-jdk8u-jdk/src/share/classes
OUTDIR=`pwd`/out
SRC=`find ${INDIR} -name '*\.java' -print`
# find checker framework annotations in source directory
ANNOS=`echo ${SRC} | xargs cat | grep 'org\.checkerframework\.' | sed 's/^.*\(org\.checkerframework\.[^); ]*\).*$/\1/g' | grep -v '\*$' | sort -u`
SCMD=""

# build up in SCMD the sed program that eliminates annotations in ${ANNOS}
for f in ${ANNOS} ; do
    QEND='\(({[^}]*})\)?'  # optional end part of annotation
    QUAL=`echo $f | sed 's/\([^.]*\.\)*/\\\\@/'`
    SCMD="s/${QUAL}${QEND}//g;s/\\@$f${QEND}//g;$SCMD"
done
# run each source file through sed
for f in `cd ${INDIR} && find * -name '*\.java' -print` ; do
    mkdir -p ${OUTDIR}/`dirname $f`
    sed ${SCMD} < ${INDIR}/$f > ${OUTDIR}/$f
done

