#!/bin/sh

myDir="`dirname $0`"
case `uname -s` in
    CYGWIN*)
      myDir=`cygpath -m $myDir`
      ;;
esac
#Fail on error status

FRAMEWORK_DIR=$myDir"/.."
MASTER=$FRAMEWORK_DIR"/framework/src/org/checkerframework/framework/util/PluginUtil.java"
ECLIPSE=$FRAMEWORK_DIR"/eclipse/checker-framework-eclipse-plugin/src/org/checkerframework/eclipse/util/PluginUtil.java"
MAVEN=$FRAMEWORK_DIR"/maven-plugin/src/main/java/org/checkerframework/mavenplugin/PluginUtil.java"

tail +2 $MASTER  &> ".PluginUtil_master.java"

head -n 1 $ECLIPSE | cat - ".PluginUtil_master.java" &> ".PluginUtil_eclipse.java"
head -n 1 $MAVEN   | cat - ".PluginUtil_master.java" &> ".PluginUtil_maven.java"

mv ".PluginUtil_eclipse.java" $ECLIPSE
mv ".PluginUtil_maven.java" $MAVEN
rm ".PluginUtil_master.java"
