#!/bin/sh
# The Checker Framework contains three identical copies of PluginUtil.java.
# This script copies one of them overtop the other two.
# It makes no attempt to synchronize changes made to different copies.

myDir="`dirname $0`"
case `uname -s` in
    CYGWIN*)
      myDir=`cygpath -m $myDir`
      ;;
esac
# Fail on error status

FRAMEWORK_DIR=$myDir"/.."
MASTER=$FRAMEWORK_DIR"/framework/src/org/checkerframework/framework/util/PluginUtil.java"
ECLIPSE=$FRAMEWORK_DIR"/eclipse/checker-framework-eclipse-plugin/src/org/checkerframework/eclipse/util/PluginUtil.java"
MAVEN=$FRAMEWORK_DIR"/maven-plugin/src/main/java/org/checkerframework/mavenplugin/PluginUtil.java"

# copy everything but the package declaration of the master file to a temporary master file
tail +2 $MASTER  &> ".PluginUtil_master.java"

# take the package declarations of the duplicates and overwrite the rest of the files for each duplicate to a tmp file
head -n 1 $ECLIPSE | cat - ".PluginUtil_master.java" &> ".PluginUtil_eclipse.java"
head -n 1 $MAVEN   | cat - ".PluginUtil_master.java" &> ".PluginUtil_maven.java"

# copy the tmp file over the normal location for the duplicates and remove the temporary files
mv ".PluginUtil_eclipse.java" $ECLIPSE
mv ".PluginUtil_maven.java" $MAVEN
rm ".PluginUtil_master.java"
