#!/bin/sh

mydir="`dirname $0`"
case `uname -s` in
    CYGWIN*)
      mydir=`cygpath -m $mydir`
      ;;
esac

checkerDist="${mydir}/../../checker/dist"
javacutilDist="${mydir}/../../javacutil/dist"
dataflowDist="${mydir}/../../dataflow/dist"

eval "mvn install:install-file -DpomFile=${mydir}/checkerQualPom.xml -Dfile=${checkerDist}/checker-qual.jar"
eval "mvn install:install-file -DpomFile=${mydir}/checkerPom.xml -Dfile=${checkerDist}/checker.jar"
eval "mvn install:install-file -DpomFile=${mydir}/compilerPom.xml -Dfile=${checkerDist}/javac.jar"
eval "mvn install:install-file -DpomFile=${mydir}/jdk8Pom.xml -Dfile=${checkerDist}/jdk8.jar"
eval "mvn install:install-file -DpomFile=${mydir}/javacutilPom.xml -Dfile=${javacutilDist}/javacutil.jar"
eval "mvn install:install-file -DpomFile=${mydir}/dataflowPom.xml -Dfile=${dataflowDist}/dataflow.jar"
