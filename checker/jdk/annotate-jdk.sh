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
#     cd checker-framework && ./gradlew assemble
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
# script.)  If successful, build8 will replace checker/dist/jdk8.jar
# with a JAR containing annotations from the annotated JDK source.
#
# To run the Checker Framework test suite:
# 0. save the newly created jdk8.jar somewhere;
# 1. check out and build annotated-jdk branch;
# 2. copy the newly created jdk8.jar to checker/dist; and
# 3. run "./gradlew build" from Checker Framework's base directory.

export SCRIPTDIR=`cd \`dirname $0\` && pwd`
export WD="`pwd`"            # run from top directory of jdk8u clone
export JDK="${WD}/jdk"       # JDK to be annotated
export TMPDIR="${WD}/tmp"    # directory for temporary files
export JAIFDIR="${WD}/jaifs" # directory for generated JAIFs
export PATCH=${SCRIPTDIR}/ad-hoc.diff

# parameters derived from environment
export PARENTDIR=`readlink -e "${CHECKERFRAMEWORK}/.."`
export AFU="${PARENTDIR}/annotation-tools"
export AFUJAR="${AFU}/annotation-file-utilities/annotation-file-utilities-all.jar"
export CFJAR="${CHECKERFRAMEWORK}/checker/dist/checker.jar"
export JAVAC="java -jar ${CHECKERFRAMEWORK}/checker/dist/checker.jar"
export JFLAGS=" -XDignore.symbol.file=true -Xmaxerrs 20000 -Xmaxwarns 20000 -source 8 -target 8 -encoding ascii"
export CLASSPATH=".:${JDK}/build/classes:${JDJAR}:${CFJAR}:${AFUJAR}:${CLASSPATH}"

# return value
export RET=0


# generate @AnnotatedFor annotations
addAnnotatedFor() {
    java org.checkerframework.framework.stub.AddAnnotatedFor
}

# find JAIFs in hierarchical directory tree and insert indicated
# annotations into corresponding source files
annotateSourceFile() {
    R=0
    BASE="${JAIFDIR}/`dirname "$1"`/`basename "$1" .java`"
    # must insert annotations on inner classes as well
    for f in ${BASE}.jaif ${BASE}\$*.jaif ; do
        if [ -r "$f" ] ; then
            insert-annotations-to-source "$f" "$1"
            [ $R -ne 0 ] || R=$?
        fi
    done
    return $R
}

# convert stubfiles to JAIF
# first arg is JAIF containing all necessary annotation definitions
convertStub() {
    java org.checkerframework.framework.stub.ToIndexFileConverter "${WD}/annotation-defs.jaif" $1
}

# convert all stubfiles in Checker Framework repository into JAIF format
# and emit to standard output
convertStubs() {
    R=0
    cd "${CHECKERFRAMEWORK}"
    [ -z "`ls`" ] && echo "no files" 1>&2 && exit 1

    for f in `find * -name 'jdk\.astub' -print` ; do
        convertStub "$f"
        [ $R -ne 0 ] || R=$?
        g="`dirname $f`/`basename $f .astub`.jaif"
        [ -r "$g" ] && cat "$g" && rm -f "$g"
    done
    return $R
}

# split up JAIF into files by package (directory) and class (JAIF)
splitJAIF() {
    awk '
        # save class sections from converted JAIFs to hierarchical JAIF dir.
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
                    if(out){fflush(out);close(out)};out=o
                    if(system("[ -s \""out"\" ]")!=0) {
                        system("mkdir -p "d" && cp "adefs" "out)
                    }
                    printf("%s\n",l)>>out  # current pkg decl
                }
                printf("%s\n",l)>>out  # current pkg decl
            }
        }
        /^annotation / { out="" }
        {if(out){print>>out}}
        END {close(out)}
    '
}

# remove all annotation definitions from JAIF
stripDefs() {
    awk '
      /^annotation / {suppress=1}
      /^class / {suppress=0}
      /^package / {suppress=0}
      {if(!suppress){print}}
    '
}


# Stage 0: restore old comments (should happen only once)

COMMENTS=0  # non-zero to enable
if [ ${COMMENTS} -ne 0 ] ; then
# download patch
[ -r annotated-jdk-comment-patch.jaif ] || wget https://checkerframework.org/annotated-jdk-comment-patch.jaif || exit $?
(cd "${JDK}" && patch -p1 < annotated-jdk-comment-patch.jaif)
fi

# download annotation definitions
[ -r annotation-defs.jaif ]\
 || wget https://checkerframework.org/annotation-defs.jaif\
 || exit $?


# Stage 1: extract JAIFs from nullness JDK

rm -rf "${TMPDIR}"
mkdir "${TMPDIR}"

(
    cd "${CHECKERFRAMEWORK}/checker/jdk/nullness/src" || exit 1
    [ -z "`ls`" ] && echo "no files" 1>&2 && exit 1

    mkdir -p ../build
    find * -name google -prune -o -name '*\.java' -print | xargs javac -d ../build ${JFLAGS}
    [ ${RET} -eq 0 ] && RET=$?
    cd ../build || exit 1

    for f in `find * -name '*\.class' -print` ; do
        extract-annotations "$f" 1>&2
        [ ${RET} -eq 0 ] && RET=$?
    done

    for f in `find * -name '*\.jaif' -print` ; do
        mkdir -p "${TMPDIR}/`dirname $f`" && mv "$f" "${TMPDIR}/$f"
        [ ${RET} -eq 0 ] && RET=$?
    done
)

#[ ${RET} -ne 0 ] && echo "stage 1 failed" 1>&2 && exit ${RET}
echo "stage 1 complete" 1>&2


# Stage 2: convert stub files to JAIFs

convertStubs | splitJAIF
RET=$?
#[ ${RET} -ne 0 ] && echo "stage 2 failed" 1>&2 && exit ${RET}
echo "stage 2 complete" 1>&2


# Stage 3: combine JAIFs from Stages 1 and 2

rm -rf "${JAIFDIR}"
# write out JAIFs from TMPDIR, replacing (bogus) annotation defs
for f in `(cd "${TMPDIR}" && find * -name '*\.jaif' -print)` ; do
    g="${JAIFDIR}/$f"
    mkdir -p `dirname $g`
    echo "$g" 1>&2
    cp "${WD}/annotation-defs.jaif" "$g"

    # first write out standard annotation defs
    # then strip out empty annotation defs
    # also generate and insert @AnnotatedFor annotations
    (cat "${WD}/annotation-defs.jaif" && stripDefs < "${TMPDIR}/$f") | addAnnotatedFor > "$g"
    [ ${RET} -ne 0 ] || RET=$?
done

#[ ${RET} -ne 0 ] && echo "stage 3 failed" 1>&2 && exit ${RET}
echo "stage 3 complete" 1>&2


# Stage 4: insert annotations from JAIFs into JDK source

(
    # first ensure source is unchanged from repo
    cd "${JDK}/src/share/classes" || exit $?
    hg revert -C com java javax jdk org sun
    rm -rf annotated

    for f in `find * -name '*\.java' -print` ; do
        annotateSourceFile $f
        [ ${RET} -ne 0 ] || RET=$?
    done

    #[ ${RET} -ne 0 ] && echo "stage 4 failed" 1>&2 && exit ${RET}

    # copy annotated source files over originals
    rsync -au annotated/* .

    # apply ad-hoc patch to correct miscellaneous errors
    if [ -r ${SCRIPTDIR}/ad-hoc.diff ] ; then
        patch -p1 < ${SCRIPTDIR}/ad-hoc.diff
    fi
)
echo "stage 4 complete" 1>&2
