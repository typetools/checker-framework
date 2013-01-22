#!/bin/sh

myDir="`dirname $0`"
case `uname -s` in
    CYGWIN*)
      myDir=`cygpath -m $myDir`
      ;;
esac


if [ "$#" -ne 1 ] || [ -d "$1" ]; then
  echo "Usage: $0 NEWVERSION" >&2
  exit 1
fi

eval "sh $myDir/updateVersion.sh" $1 $myDir"/checkersQualsPom.xml"
eval "sh $myDir/updateVersion.sh" $1 $myDir"/checkersPom.xml"
eval "sh $myDir/updateVersion.sh" $1 $myDir"/compilerPom.xml"
eval "sh $myDir/updateVersion.sh" $1 $myDir"/jdk6Pom.xml"
eval "sh $myDir/updateVersion.sh" $1 $myDir"/jdk7Pom.xml"
eval "sh $myDir/updateVersion.sh" $1 $myDir"/../../../maven-plugin/pom.xml"