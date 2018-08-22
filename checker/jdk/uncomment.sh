#!/bin/sh

set -o pipefail

[ $# -ne 1 ] && echo "usage: `basename $0` sourcefile.java" && exit 1
[ -z "${CHECKERFRAMEWORK}" ] && echo "CHECKERFRAMEWORK not set" && exit 1

# annotated-jdk8u-jdk should be Checker Framework sibling
PARENTDIR=`(cd "${CHECKERFRAMEWORK}/.." && pwd)`
WORKDIR=`(cd "\`dirname $0\`" && pwd)`
SRCDIR="${PARENTDIR}/annotated-jdk8u-jdk/src/share/classes"
ORIGINAL="${SRCDIR}/$1"
BASE=`basename "$1" .java`
COPY="${BASE}.0"
COUNT=`expr \`grep -c cf-bug "${ORIGINAL}"\` + 0`
CURRENT=`expr 0`

# remove $1th added comment
uncomment() {
  awk -v n="$1" '
    BEGIN { n=n+0; i=0; c=0 }
    /^\/\// { if (i>0) { printf("%s\n",substr($0,3)); next } }
    /^$/ { if (i>0) { i=0; next } }
    {
      i=match($0,/throw new RuntimeException[(]"cf-bug"[)]/)
      if (i>1) {
        if (++c==n) {
          printf("%s%s\n",substr($0,0,i-1),substr($0,i+42)); next
        }
        i=0
      }
      print
    }
  '
}

cp "${ORIGINAL}" "${COPY}"
(
trap "cp ${COPY} ${ORIGINAL} && exit 1" SIGINT
while [ ${CURRENT} -le ${COUNT} ] ; do
  CURRENT=`expr ${CURRENT} + 1`
  OUT="${BASE}.${CURRENT}"
  uncomment ${CURRENT} < "${COPY}" > "${OUT}"
  cp "${OUT}" "${ORIGINAL}"
  # call oaat.sh ("one at a time") to compile current file
  (bash "${WORKDIR}/oaat.sh" "$1" 2>&1 | tee "${CURRENT}.log") || true
done
cp "${COPY}" "${ORIGINAL}"
)
