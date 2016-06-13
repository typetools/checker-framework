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
# script.)  If successful, build8 will replace checker/dist/jdk8.jar
# with a JAR containing annotations from the annotated JDK source.
#
# To run the Checker Framework test suite:
# 0. save the newly created jdk8.jar somewhere;
# 1. check out and build annotated-jdk branch;
# 2. copy the newly created jdk8.jar to checker/dist; and
# 3. run "ant tests-nobuildjdk" from Checker Framework's base directory.

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
export JFLAGS="-XDignore.symbol.file=true -Xmaxerrs 20000 -Xmaxwarns 20000 -source 8 -target 8 -encoding ascii -cp ${CP}"

# return value
export RET=0


addAnnotatedFor() {
    java -cp ${CP} org.checkerframework.framework.stub.AddAnnotatedFor
}

convertStub() {
    java -cp "${CP}" org.checkerframework.framework.stub.ToIndexFileConverter "${WD}/annotation-defs.jaif" "$1"
}

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
[ -r annotated-jdk-comment-patch.jaif ] || wget https://types.cs.washington.edu/checker-framework/annotated-jdk-comment-patch.jaif || exit $?
(cd "${JDK}" && patch -p1 < annotated-jdk-comment-patch.jaif)
fi

# download annotation definitions
[ -r annotation-defs.jaif ]\
 || wget https://types.cs.washington.edu/checker-framework/annotation-defs.jaif\
 || exit $?


(
cat "${WD}/annotation-defs.jaif"

# Stage 1: extract JAIFs from nullness JDK

(
    cd "${CHECKERFRAMEWORK}/checker/jdk/nullness/src" || exit 1
    [ -z "`ls`" ] && echo "no files" 1>&2 && exit 1

    find * -name '*\.src' -print | xargs javac -d ../build ${JFLAGS}
    [ ${RET} -eq 0 ] && RET=$?
    cd ../build || exit 1

    for f in `find * -name '*\.class' -print` ; do
        CLASSPATH="${CP}" extract-annotations "$f" 1>&2
        [ ${RET} -eq 0 ] && RET=$?
    done

    for f in `find * -name '*\.jaif' -print` ; do
        cat "$f" && rm -f "$f"
        [ ${RET} -eq 0 ] && RET=$?
    done
)

[ ${RET} -ne 0 ] && echo "stage 1 failed" 1>&2 && exit ${RET}
echo "stage 1 complete" 1>&2


# Stage 2: convert stub files to JAIFs

cd "${CHECKERFRAMEWORK}"
[ -z "`ls`" ] && echo "no files" 1>&2 && exit 1

for f in `find * -name 'jdk\.astub' -print` ; do
    convertStub "$f"
    [ ${RET} -ne 0 ] || RET=$?
    g="`dirname $f`/`basename $f .astub`.jaif"
    [ -r "$g" ] && cat "$g" && rm -f "$g"
done

[ ${RET} -ne 0 ] && echo "stage 2 failed" 1>&2 && exit ${RET}
echo "stage 2 complete" 1>&2


# Stage 3: combine JAIFs from Stages 1 and 2
) | stripDefs > "${WD}/temp.jaif"

# middle line is hack to get around bug of unknown origin
cat "${WD}/annotation-defs.jaif" "${WD}/temp.jaif"\
 | sed 's/<clinit>:/<clinit>()V:/'\
 | addAnnotatedFor > "${WD}/jdk.jaif"


# Stage 4: insert annotations from JAIFs into JDK source

(
    # first ensure source is unchanged from repo
    cd "${JDK}/src/share/classes" || exit $?
    hg revert -C com java javax jdk org sun
    rm -rf annotated

    for f in `find * -name '*\.java' -print` ; do
        CLASSPATH=${CP} insert-annotations-to-source "${WD}/jdk.jaif" "$f"
        [ ${RET} -ne 0 ] || RET=$?
    done

    # copy annotated source files over originals
    rsync -au annotated/* .

    # apply ad-hoc patch to correct miscellaneous errors
    if [ -r ${SCRIPTDIR}/ad-hoc.diff ] ; then
        patch -p1 < ${SCRIPTDIR}/ad-hoc.diff
    fi
)

