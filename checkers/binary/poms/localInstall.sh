#!/bin/sh

mydir="`dirname $0`"
case `uname -s` in
    CYGWIN*)
      mydir=`cygpath -m $mydir`
      ;;
esac

binDir="${mydir}/.."

eval "mvn install:install-file -DpomFile=checkerQualsPom.xml -Dfile=${binDir}/checkers-quals.jar"
eval "mvn install:install-file -DpomFile=checkersPom.xml -Dfile=${binDir}/checkers.jar"
eval "mvn install:install-file -DpomFile=compilerPom.xml -Dfile=${binDir}/javac.jar"
eval "mvn install:install-file -DpomFile=jdk6Pom.xml -Dfile=${binDir}/jdk6.jar"
eval "mvn install:install-file -DpomFile=jdk7Pom.xml -Dfile=${binDir}/jdk7.jar"