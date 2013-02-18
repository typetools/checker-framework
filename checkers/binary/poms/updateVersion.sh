#!/bin/sh

mydir="`dirname $0`"
case `uname -s` in
    CYGWIN*)
      mydir=`cygpath -m $mydir`
      ;;
esac

if [ "$#" -ne 2 ] || [ -d "$2" ]; then
  echo "Usage: $0 NEWVERSION POMFILE" >&2
  exit 1
fi

MATCH="\<version\>\<\!-- SUBST -->.*\<\!-- \/SUBST --><\/version>"
REPLACE="<version><!-- SUBST -->$1<!-- \/SUBST --><\/version>"

#I am using &> rather than -i since there seems to be
#issues with -i on the Mac
SEDSTMT="sed -i.bc 's/$MATCH/$REPLACE/g' $2"
RM_BACKUP="rm $2.bc"

eval $SEDSTMT
eval $RM_BACKUP