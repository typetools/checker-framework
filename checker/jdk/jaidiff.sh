#!/bin/sh

# "set difference" for JAIFs

# arguments are two JAIFs, in order: "minuend" and "subtrahend"; result
# is JAIF containing all and only annotations in minuend and not in
# subtrahend, written to standard output

# parameters all derived from CHECKERFRAMEWORK
JSR308="`cd $CHECKERFRAMEWORK/.. && pwd`"   # base directory
AFU_HOME="${JSR308}/annotation-tools"
AFU="${AFU_HOME}/annotation-file-utilities"
AFU_JAR="${AFU}/annotation-file-utilities.jar"
ASM=${AFU_HOME}/asmx/bin
#ASM=${AFU}/lib/asm-5.0.jar
PLUME="${AFU}/lib/plume.jar"
CP="${AFU}:${AFU}/bin:${AFU_HOME}/scene-lib/bin:${ASM}:${AFU_JAR}"

java -ea -cp ${CP}:${CLASSPATH} annotations.util.SceneOps diff $1 $2

