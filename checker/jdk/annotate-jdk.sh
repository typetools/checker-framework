#!/bin/sh

# Generates the annotated JDK from old annotation sources (lock and
# nullness JDKs and stubfiles).  The goal is to transfer all the
# annotations from the sources to the JDK source, which will be the new
# home for all the annotations associated with the checkers that are
# distributed with the Checker Framework.

# To run:
#
# 1.  Clone and build the checker framework from source.
#     git clone https://github.com/typetools/checker-framework
#     cd checker-framework
#     git checkout create-annotated-jdk
#     ant
#
# 2.  Clone the OpenJDK 8u repository and sub-repositories.
#     hg clone http://hg.openjdk.java.net/jdk8u/jdk8u  [yes, jdk8u*2]
#     cd jdk8u && sh ./get_source
#
# 3.  Run this script from the top-level OpenJDK directory
#     ("jdk8u" by default); this takes about 2 hours.
#     The first time you do so, edit this script to change "COMMENTS=0"
#     to "COMMENTS=1", then edit the script to set it back afterward.
#     .../checker-framework/checker/jdk/annotate-jdk.sh
#
# 4.  Compile the annotated JDK 8 source; this takes about 9 hours.
#     .../checker-framework/checker/jdk/build-jdk-jar.sh
#     (It may be necessary to edit some of the variable settings in the
#     script.)  If successful, this will replace checker/dist/jdk8.jar
#     with a .jar file containing annotations from the annotated JDK source.
#   
# 5.  Run the Checker Framework test suite
#     0. save the newly created jdk8.jar somewhere;
#     1. check out and build annotated-jdk branch;
#     2. copy the newly created jdk8.jar to checker/dist; and
#     3. run "ant tests-nobuildjdk" from Checker Framework's base directory.


# Build stages for this script:
#
# 0.  Restore comments from old nullness JDK and stubfiles.
#     (These comments explain non-intuitive annotation choices, etc.
#     This stage should run only once.  You have to edit this file
#     to make this happen.)
#
# 1.  Extract annotations from the lock and nullness JDKs into JAIFs.
#
# 2.  Convert old stubfiles into JAIFs.
#
# 3.  Combine the results of the previous two stages.
#
# 4.  Insert annotations from JAIFs into JDK source files.


[ -r ${CHECKERFRAMEWORK} ] || exit 1

export SCRIPTDIR=`dirname \`readlink -m -v $0\``
export WD="`pwd`"            # run from top directory of jdk8u clone
export JDK="${WD}/jdk"       # JDK to be annotated
export TMPDIR="${WD}/tmp"    # directory for temporary files
export JAIFDIR="${WD}/jaifs" # directory for generated JAIFs
export PATCH=${SCRIPTDIR}/ad-hoc.diff
export ADEFS=${SCRIPTDIR}/annotation-defs.jaif

# parameters derived from environment
export JSR308=`[ -d "${CHECKERFRAMEWORK}" ] && cd "${CHECKERFRAMEWORK}/.." && pwd`
export AFU="${JSR308}/annotation-tools"
export AFUJAR="${AFU}/annotation-file-utilities/annotation-file-utilities.jar"
export CFJAR="${CHECKERFRAMEWORK}/checker/dist/checker.jar"
export LTJAR="${JSR308}/jsr308-langtools/dist/lib/javac.jar"
export JDJAR="${JSR308}/jsr308-langtools/dist/lib/javadoc.jar"
export JAVAC="java -jar ${CHECKERFRAMEWORK}/checker/dist/checker.jar"
export JFLAGS="-Xbootclasspath/p:${CLASSPATH}/checker/dist/javac.jar -XDignore.symbol.file=true -Xmaxerrs 20000 -Xmaxwarns 20000 -source 8 -target 8 -encoding ascii"
export CLASSPATH=".:${JDK}/build/classes:${LTJAR}:${JDJAR}:${CFJAR}:${AFUJAR}:${CLASSPATH}"

# return value
export RET=0


# Generate @AnnotatedFor annotations.
# Reads from stdin and writes to stdout.
addAnnotatedFor() {
    java org.checkerframework.framework.stub.AddAnnotatedFor
}

# Find JAIFs in hierarchical directory tree and insert the JAIFs'
# annotations into corresponding source files.
# Takes one argument, a Java source file.
# Returns non-zero if a command failed.
annotateSourceFile() {
    R=0
    JAIFBASE="${JAIFDIR}/`dirname "$1"`/`basename "$1" .java`"
    # must insert annotations on inner classes as well
    for f in ${JAIFBASE}.jaif ${JAIFBASE}\$*.jaif ; do
        if [ -r "$f" ] ; then
            insert-annotations-to-source "$f" "$1"
            [ $R -ne 0 ] || R=$?
        fi
    done
    return $R
}

# Convert stubfile to JAIF.
# Returns non-zero if a command failed.
convertStub() {
    # First arg is JAIF containing all necessary annotation definitions.
    java org.checkerframework.framework.stub.ToIndexFileConverter "${ADEFS}" $1
}

# Convert all jdk.astub stubfiles in Checker Framework repository into JAIF format
# and emit to standard output.
# Takes no arguments.
convertStubs() {
    R=0
    cd "${CHECKERFRAMEWORK}"
    [ -z "`ls`" ] && echo "no files" 1>&2 && exit 1

    for f in `find * -name 'jdk\.astub' -print` ; do
        convertStub "$f"
        if [ $? ] ; then
            g="`dirname $f`/`basename $f .astub`.jaif"
            cat "$g" && rm -f "$g"
        else
            [ $R -ne 0 ] || R=$?
        fi
    done
    return $R
}

# Split up JAIF (piped in from stdin) into files by package (directory) and
# class (JAIF), in $TMPDIR.
splitJAIF() {
    awk '
        # save class sections from converted JAIFs to hierarchical JAIF dir.
        BEGIN {out="";adefs=ENVIRON["ADEFS"]}
        /^package / {
            packageline=$0;
            colonindex=index($2,":");
            packagedir=(colonindex?substr($2,1,colonindex-1):$2)
            if(packagedir){gsub(/\./,"/",packagedir)}else{packagedir=""}
            packagedir=ENVIRON["TMPDIR"]"/"packagedir
        }
        /^class / {
            colonindex=index($2,":");
            c=(colonindex?substr($2,1,colonindex-1):$2)
            if(c) {
                o=packagedir"/"c".jaif"
                if (o!=out) {
                    if(out){fflush(out);close(out)};
                    out=o
                    if(system("[ -s \""out"\" ]")!=0) {
                        system("mkdir -p "packagedir" && cp "adefs" "out)
                    }
                    printf("%s\n",packageline)>>out  # current pkg decl
                }
                printf("%s\n",packageline)>>out  # current pkg decl
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
    (cd "${JDK}" && patch -p1 < ${SCRIPTDIR}/annotated-jdk-comment-patch.jaif)
fi


# Stage 1: extract JAIFs from lock and nullness JDKs to ${TMPDIR}

rm -rf "${TMPDIR}"
mkdir "${TMPDIR}"

for typesystem in lock nullness ; do
    cd "${CHECKERFRAMEWORK}/checker/jdk/$typesystem/src" || exit 1
    [ -z "`ls`" ] && echo "no files" 1>&2 && exit 1

    mkdir -p ../build
    find * -name google -prune -o -name '*\.java' -print | xargs javac -d ../build ${JFLAGS}
    [ ${RET} -eq 0 ] && RET=$?
    cd ../build || exit 1

    for f in `find * -name '*\.class' -print` ; do
        extract-annotations -b "$f" 1>&2
        [ ${RET} -eq 0 ] && RET=$?
    done

    for f in `find * -name '*\.jaif' -print` ; do
        mkdir -p "${TMPDIR}/`dirname $f`" && cat "$f" >> "${TMPDIR}/$f"
        [ ${RET} -eq 0 ] && RET=$?
    done
done

[ ${RET} -ne 0 ] && echo "stage 1 failed" 1>&2 && exit ${RET}
echo "stage 1 complete" 1>&2


# Stage 2: convert stub files to JAIFs

# sed invocation is temporary workaround for stubfile converter bug
convertStubs | sed 's/<clinit>:/<clinit>()V/' | splitJAIF
RET=$?
[ ${RET} -ne 0 ] && echo "stage 2 failed" 1>&2 && exit ${RET}
echo "stage 2 complete" 1>&2


# Stage 3: combine JAIFs from Stages 1 and 2

rm -rf "${JAIFDIR}"
# Write out JAIFs from TMPDIR, replacing (bogus) annotation defs from
# stubfile converter which makes up empty definitions.
for f in `(cd "${TMPDIR}" && find * -name '*\.jaif' -print)` ; do
    g="${JAIFDIR}/$f"
    mkdir -p `dirname $g`
    echo "$g" 1>&2
    cp "${ADEFS}" "$g"

    # first write out standard annotation defs
    # then strip out empty annotation defs
    # also generate and insert @AnnotatedFor annotations
    (cat "${ADEFS}" && stripDefs < "${TMPDIR}/$f") | addAnnotatedFor > "$g"
    [ ${RET} -ne 0 ] || RET=$?
done

[ ${RET} -ne 0 ] && echo "stage 3 failed" 1>&2 && exit ${RET}
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

    [ ${RET} -ne 0 ] && echo "stage 4 failed" 1>&2 && exit ${RET}

    # copy annotated source files over originals
    rsync -au annotated/* .

    # apply ad-hoc patch to correct miscellaneous errors
    if [ -r ${PATCH} ] ; then
        patch -p1 < ${PATCH}
    fi
)
echo "stage 4 complete" 1>&2
