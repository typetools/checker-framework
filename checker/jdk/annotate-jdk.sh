#!/bin/sh

# Generates the annotated JDK from old annotation sources (nullness JDK
# and stubfiles).  The goal is to transfer all the annotations from the
# sources to the JDK source, which will be the new home for all the
# annotations associated with the checkers that are distributed with the
# Checker Framework.
#
# Prerequisites:
#
# 1.  Clone and build the checker framework from source.
#     git clone https://github.com/typetools/checker-framework
#     cd checker-framework && ant
#     
# 2.  Clone the OpenJDK 8u repository and sub-repositories.
#     hg clone http://hg.openjdk.java.net/jdk8u/jdk8u  [yes, jdk8u*2]
#     cd jdk8u && sh ./get_source
#
# This script should be run from the top-level OpenJDK directory
# ("jdk8u" by default).
#
# 
# Build stages:
#
# 0.  Restore comments from old nullness JDK and stubfiles.
#     (These comments explain non-intuitive annotation choices, etc.
#     This stage should run only once.)
#
# 1.  Extract annotations from the nullness JDK into JAIFs.
#
# 2.  Convert old stubfiles into JAIFs.
#
# 3.  Combine the results of the previous two stages.
#
# 4.  Insert annotations from JAIFs into JDK source files.
#
#
# The end product of these stages is the annotated JDK 8 source.  To
# build, invoke the Checker Framework script checker/jdk/build8.sh.
# (It may be necessary to edit some of the variable settings in the
# script.)

export SCRIPTDIR=`cd \`dirname $0\` && pwd`
export WD="`pwd`"            # run from top directory of jdk8u clone
export JDK="${WD}/jdk"       # JDK to be annotated
export TMPDIR="${WD}/tmp"    # directory for temporary files
export JAIFDIR="${WD}/jaifs" # directory for generated JAIFs
export PATCH=${SCRIPTDIR}/ad-hoc.diff

# parameters derived from environment
export JSR308=`[ -d "${CHECKERFRAMEWORK}" ] && cd "${CHECKERFRAMEWORK}/.." && pwd`
export AFU="${JSR308}/annotation-tools"
export AFUJAR="${AFU}/annotation-file-utilities/annotation-file-utilities.jar"
export CFJAR="${CHECKERFRAMEWORK}/checker/dist/checker.jar"
export LTJAR="${JSR308}/jsr308-langtools/dist/lib/javac.jar"
export JDJAR="${JSR308}/jsr308-langtools/dist/lib/javadoc.jar"
export CP=".:${JDK}/build/classes:${LTJAR}:${JDJAR}:${CFJAR}:${AFUJAR}:${CLASSPATH}"

# return value
export RET=0


# Stage 0: restore old comments (should happen only once)

COMMENTS=0  # non-zero to enable
if [ ${COMMENTS} -ne 0 ] ; then
# download patch
[ -r annotated-jdk-comment-patch.jaif ] || wget https://types.cs.washington.edu/checker-framework/annotated-jdk-comment-patch.jaif || exit $?
(cd "${JDK}" && patch -p1 < annotated-jdk-comment-patch.jaif)
fi


# Stage 1: extract JAIFs from nullness JDK

rm -rf "${TMPDIR}"
mkdir "${TMPDIR}"

(
    cd "${CHECKERFRAMEWORK}/checker/jdk/nullness/build" || exit 1
    [ -z "`ls`" ] && echo "no files" 1>&2 && exit 1

    for f in `find * -name '*\.class' -print` ; do
        CLASSPATH="${CP}" extract-annotations "$f" 1>&2
        [ ${RET} -eq 0 ] && RET=$?
    done

    for f in `find * -name '*\.jaif' -print` ; do
        mkdir -p "${TMPDIR}/`dirname $f`" && mv "$f" "${TMPDIR}/$f"
        [ ${RET} -eq 0 ] && RET=$?
    done
)

[ ${RET} -ne 0 ] && echo "stage 1 failed" 1>&2 && exit ${RET}


# Stage 2: convert stub files to JAIFs

# download annotation definitions
[ -r annotation-defs.jaif ]\
 || wget https://types.cs.washington.edu/checker-framework/annotation-defs.jaif\
 || exit $?

(
    cd "${CHECKERFRAMEWORK}"
    [ -z "`ls`" ] && echo "no files" 1>&2 && exit 1

    for f in `find * -name 'jdk\.astub' -print` ; do
        java -cp "${CP}" org.checkerframework.framework.stub.ToIndexFileConverter "$f"
        x=$?
        [ ${RET} -ne 0 ] || RET=$x
        g="`dirname $f`/`basename $f .astub`.jaif"
        [ -r "$g" ] && cat "$g" && rm -f "$g"
    done
) | awk '
    # save class sections from converted JAIFs to hierarchical JAIF directory
    BEGIN {out="";adefs=ENVIRON["WD"]"/annotation-defs.jaif"}
    /^package / {
        l=$0;i=index($2,":");d=(i?substr($2,1,i-1):$2)
        if(d){gsub(/\./,"/",d)}else{d=""}
        d=ENVIRON["TMPDIR"]"/"d
    }
    /^class / {
        i=index($2,":");c=(i?substr($2,1,i-1):$2)
        if(c) {
            o=d"/"c".jaif"
            if (o!=out) {
                if(out){close(out)};out=o
                if(system("test -s "out)!=0) {
                    system("mkdir -p "d" && cp "adefs" "out)
                }
                printf("%s\n",l)>>out  # current pkg decl
            }
        }
    }
    {if(out){print>>out}}
    END {close(out)}
'

[ ${RET} -ne 0 ] && echo "stage 2 failed" 1>&2 && exit ${RET}


# Stage 3: combine JAIFs from Stages 1 and 2

(
    rm -rf "${JAIFDIR}"
    # write out JAIFs from TMPDIR, replacing (bogus) annotation defs
    for f in `(cd "${TMPDIR}" && find * -name '*\.jaif' -print)` ; do
        g="${JAIFDIR}/$f"
        mkdir -p `dirname $g`
        echo "$g"

        # first write out standard annotation defs
        # then strip out empty annotation defs
        # also generate and insert @AnnotatedFor annotations
        awk '
            # initial state: print on, no class seen yet
            BEGIN {x=2}
            # skip until class or package (unless no class seen yet)
            /^annotation/ {if(x<=1){x=-1}}
            # hold and print only if class follows (unless no class seen yet)
            /^package/ {if(x<=1){x=0;i=0;split("",a)}}
            # print stored lines, note class has been seen (x=1 instead of 2)
            /^class/ {for(j=0;j<i;++j){print a[j]};split("",a);x=1;i=0}
            # store, print, or drop (depending on x)
            {if(x==0){a[i++]=$0}{if(x>0)print}}
        ' < "${TMPDIR}/$f" | java -cp ${CP} org.checkerframework.framework.stub.AddAnnotatedFor > "$g"
    done
)

[ ${RET} -ne 0 ] && echo "stage 3 failed" 1>&2 && exit ${RET}


# Stage 4: insert annotations from JAIFs into JDK source

(
    # first ensure source is unchanged from repo
    cd "${JDK}/src/share/classes" || exit $?
    hg revert -C com java javax jdk org sun
    rm -rf annotated

    for f in `find * -name '*\.java' -print` ; do
        BASE="${JAIFDIR}/`dirname $f`/`basename $f .java`"
        # must insert annotations on inner classes as well
        for g in ${BASE}.jaif ${BASE}\$*.jaif ; do
            if [ -r "$g" ] ; then
                CLASSPATH=${CP} insert-annotations-to-source "$g" "$f"
                [ ${RET} -ne 0 ] || RET=$?
            fi
        done
    done

    [ ${RET} -ne 0 ] && echo "stage 4 failed" 1>&2 && exit ${RET}
    # copy annotated source files over originals
    rsync -au annotated/* .
    # apply ad-hoc patch to correct miscellaneous errors
    if [ -r ${SCRIPTDIR}/ad-hoc.diff ] ; then
        patch -p1 < ${SCRIPTDIR}/ad-hoc.diff
    fi
)

