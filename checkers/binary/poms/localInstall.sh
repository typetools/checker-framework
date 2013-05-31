#!/bin/sh

mydir="`dirname $0`"
case `uname -s` in
    CYGWIN*)
      mydir=`cygpath -m $mydir`
      ;;
esac

binDir="${mydir}/.."

eval "mvn install:install-file -DpomFile=${mydir}/checkersQualsPom.xml -Dfile=${binDir}/checkers-quals.jar"
eval "mvn install:install-file -DpomFile=${mydir}/checkersPom.xml -Dfile=${binDir}/checkers.jar"
eval "mvn install:install-file -DpomFile=${mydir}/compilerPom.xml -Dfile=${binDir}/javac.jar"
eval "mvn install:install-file -DpomFile=${mydir}/jdk7Pom.xml -Dfile=${binDir}/jdk7.jar"