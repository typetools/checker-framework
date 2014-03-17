#!/bin/sh

MASTER=$CHECKERFRAMEWORK"/framework/src/org/checkerframework/framework/util/PluginUtil.java"
ECLIPSE=$CHECKERFRAMEWORK"/eclipse/checker-framework-eclipse-plugin/src/checkers/eclipse/util/PluginUtil.java"
MAVEN=$CHECKERFRAMEWORK"/maven-plugin/src/main/java/org/checkerframework/mavenplugin/PluginUtil.java"

tail +2 $MASTER  &> ".PluginUtil_master.java"

head -n 1 $ECLIPSE | cat - ".PluginUtil_master.java" &> ".PluginUtil_eclipse.java"
head -n 1 $MAVEN   | cat - ".PluginUtil_master.java" &> ".PluginUtil_maven.java"

mv ".PluginUtil_eclipse.java" $ECLIPSE
mv ".PluginUtil_maven.java" $MAVEN
rm ".PluginUtil_master.java"
