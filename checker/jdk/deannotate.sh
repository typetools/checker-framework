#!/bin/sh

# strip Checker Framework annotations out of source files

INDIR=$JSR308/annotated-jdk8u-jdk/src/share/classes  # source directory
OUTDIR=`pwd`/out                                     # output directory
SRC=`find ${INDIR} -name '*\.java' -print`           # source files

# find checker framework annotations in source directory
ANNOS=`echo ${SRC} | xargs cat | grep 'org\.checkerframework\.' | sed 's/^.*\(org\.checkerframework\.[^); ]*\).*$/\1/g' | grep -v '\*$' | sort -u`
QEND='\((\([^()]\|()\)*)\)\?'  # optionally matches annotation arguments
SCMD=""

# build up in SCMD the sed program that eliminates annotations in ${ANNOS}
for f in ${ANNOS} ; do
    # add both qualified and unqualified versions to SCMD
    QUAL=`echo $f | sed 's/\([^.]*\.\)*/\\\\@/'`
    SCMD="s/${QUAL}${QEND}//g;s/\\@$f${QEND}//g;$SCMD"
done

# run each source file through sed
for f in `cd ${INDIR} && find * -name '*\.java' -print` ; do
    mkdir -p ${OUTDIR}/`dirname $f`
    sed ${SCMD} < ${INDIR}/$f > ${OUTDIR}/$f
done

