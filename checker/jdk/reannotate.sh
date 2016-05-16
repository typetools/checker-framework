#!/bin/sh

# insert JAIFs into source files in parallel directory tree

# add annotations to INDIR source to produce OUTDIR source
WORKDIR=`cd \`dirname $0\` && pwd`
INDIR=${WORKDIR}/deannotated-jdk
OUTDIR=${WORKDIR}/reannotated-jdk
JAIFDIR=${WORKDIR}/jdiffs/d

# strip filename of extension and any '$' suffixes
base() {
    for f in $* ; do
        d=`dirname $f`
        b="`basename $f .jaif | sed 's/\$.*$//'`.jaif"
        echo "$d/$b"
    done
}

for f in `base \`(cd ${JAIFDIR} && find * -name '*\.jaif' -print)\` | sort -u`
do
    d=`dirname $f`
    b="`basename $f .jaif | sed 's/\$.*$//'`"
    # find JAIFs for a class and its local classes
    JAIFS=`ls ${JAIFDIR}/$d/$b{,\\$*}.jaif 2>/dev/null`
    SRC=${INDIR}/$d/$b.java
    [ -z "${JAIFS}" ] && continue
    echo ${SRC}
    insert-annotations-to-source ${JAIFS} ${SRC}
done
rm -rf ${OUTDIR}
mv annotated ${OUTDIR}

