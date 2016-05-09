#!/bin/sh
# The Checker Framework contains three identical copeis of PluginUtil.java.
# This script exits with non-zero status if the copies differ.

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

tail -n +2 $MASTER   &> ".PluginUtil_master.java"
tail -n +2 $ECLIPSE  &> ".PluginUtil_eclipse.java"
tail -n +2 $MAVEN    &> ".PluginUtil_maven.java"

diff --brief ".PluginUtil_master.java" ".PluginUtil_eclipse.java"
rcEclipse=$?

diff --brief ".PluginUtil_master.java" ".PluginUtil_maven.java"
rcMaven=$?

rm ".PluginUtil_eclipse.java"
rm ".PluginUtil_maven.java"
rm ".PluginUtil_master.java"

if [[ $rcEclipse != 0 ]] ; then
    echo "Eclipse PluginUtil.java differs from Checker Framework version"
    echo $ECLIPSE" differs from\n"$MASTER
    exit 1
fi

if [[ $rcMaven != 0 ]] ; then
    echo "Maven PluginUtil.java differs from Checker Framework version"
    echo $MAVEN" differs from\n"$MASTER
    exit 1
fi
