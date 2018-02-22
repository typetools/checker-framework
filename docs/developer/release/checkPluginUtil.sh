#!/bin/bash
# The Checker Framework contains three identical copies of PluginUtil.java.
# This script exits with non-zero status if the copies differ.

# Fail if any command fails
set -e

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

diff -q <(tail -n +2 $MASTER) <(tail -n +2 $ECLIPSE) >& /dev/null || (echo -e "Files differ:\n  $MASTER\n  $ECLIPSE" && false)
