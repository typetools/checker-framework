#!/bin/sh

MASTER=$CHECKERS"/src/checkers/util/PluginUtil.java"
ECLIPSE=$CHECKERS"/../eclipse/checker-framework-eclipse-plugin/src/checkers/eclipse/util/PluginUtil.java"
MAVEN=$CHECKERS"/../maven-plugin/src/main/java/org/checkersplugin/PluginUtil.java"

tail +2 $MASTER  &> ".PluginUtil_master.java"

head -n 1 $ECLIPSE | cat - ".PluginUtil_master.java" &> ".PluginUtil_eclipse.java"
head -n 1 $MAVEN   | cat - ".PluginUtil_master.java" &> ".PluginUtil_maven.java"

mv ".PluginUtil_eclipse.java" $ECLIPSE
mv ".PluginUtil_maven.java" $MAVEN
rm ".PluginUtil_master.java"