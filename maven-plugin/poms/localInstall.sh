#!/bin/sh

mydir="`dirname $0`"
case `uname -s` in
    CYGWIN*)
      mydir=`cygpath -m $mydir`
      ;;
esac

checkerDist="${mydir}/../../checker/dist"

eval "mvn install:install-file -DpomFile=${mydir}/checkerQualPom.xml -Dfile=${checkerDist}/checker-qual.jar"
eval "mvn install:install-file -DpomFile=${mydir}/checkerPom.xml -Dfile=${checkerDist}/checker.jar"
eval "mvn install:install-file -DpomFile=${mydir}/compilerPom.xml -Dfile=${checkerDist}/javac.jar"
eval "mvn install:install-file -DpomFile=${mydir}/jdk7Pom.xml -Dfile=${checkerDist}/jdk7.jar"