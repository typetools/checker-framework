#!/bin/sh

## This is an executable example that runs wpi-many.sh with appropriate
## arguments and environment variables, using a custom typechecker.
## It exercises most of the features of the wpi-many.sh script.
##
## Rather than directly using this script, you should copy it and
## then modify and run your copy. Every line of the script before
## the line that says "There is no need to make changes below this point"
## defines a variable (either an environment variable or a variable
## used as input to wpi-many.sh). You should consider changing each
## of these, to suit your use case.

## Change these to match your system.

export JAVA8_HOME=/usr/lib/jvm/java-1.8.0-openjdk
export JAVA11_HOME=/usr/lib/jvm/java-11-openjdk
export JAVA16_HOME=/usr/lib/jvm/java-16-oracle

export ANDROID_HOME=${HOME}/compliance-experiments/fse20/android_home

# This directory must contain a copy of the Checker Framework that has
# been built from source.
export CHECKERFRAMEWORK=${HOME}/jsr308/checker-framework

## Change these to match your experimental setup.

export PARENTDIR=${HOME}/compliance-experiments/fse20
checker=org.checkerframework.checker.noliteral.NoLiteralChecker
checkername=no-literal
repolist=securerandom.list
workingdir=$(pwd)
timeout=3600 # 60 minutes

# The stub files for the checker being used.
custom_stubs=${PARENTDIR}/no-literal-checker/no-literal-checker/stubs

# The checker classpath.  Paste in the result of running ./gradlew -q
# printClasspath in the subproject of your custom checker with the
# checker implementation.  If your custom checker does not define such
# a task, you can define it:
#
# task printClasspath {
#     doLast {
#         println sourceSets.main.runtimeClasspath.asPath
#     }
# }
#
# If you are not using a custom typechecker (i.e. you are using a typechecker built into
# the Checker Framework, such as the Nullness Checker), set this variable to the empty string
# or comment out this line.
checker_classpath='/homes/gws/kelloggm/compliance-experiments/fse20/no-literal-checker/no-literal-checker/build/classes/java/main:/homes/gws/kelloggm/compliance-experiments/fse20/no-literal-checker/no-literal-checker/build/resources/main:/homes/gws/kelloggm/compliance-experiments/fse20/checker-framework/checker/dist/checker.jar:/homes/gws/kelloggm/compliance-experiments/fse20/no-literal-checker/no-literal-qual/build/libs/no-literal-qual.jar:/homes/gws/kelloggm/.gradle/caches/modules-2/files-2.1/com.google.errorprone/javac/9+181-r4173-1/bdf4c0aa7d540ee1f7bf14d47447aea4bbf450c5/javac-9+181-r4173-1.jar:/homes/gws/kelloggm/.gradle/caches/modules-2/files-2.1/org.checkerframework/checker-qual/3.1.1/361404eff7f971a296020d47c928905b3b9c5b5f/checker-qual-3.1.1.jar'

# The qualifier classpath. Usually, this is a subset of
# checker_classpath that contains just two elements:
#  * the qual jar for your checker, and
#  * the version of checker-qual.jar that your qualifiers depend on.
#
# Like checker_classpath, this is usually generated using the printClasspath
# task in the qualifier subproject of your custom checker, if it has one.
#
# If you are not using a custom typechecker (i.e. you are using a typechecker built into
# the Checker Framework, such as the Nullness Checker), set this variable to the empty string
## or comment out this line.
qual_classpath='/homes/gws/kelloggm/compliance-experiments/fse20/no-literal-checker/no-literal-qual/build/libs/no-literal-qual.jar:/homes/gws/kelloggm/.gradle/caches/modules-2/files-2.1/org.checkerframework/checker-qual/3.1.1/361404eff7f971a296020d47c928905b3b9c5b5f/checker-qual-3.1.1.jar'

## There is no need to make changes below this point.

export JAVA_HOME=${JAVA11_HOME}
repolistbase=$(basename "$repolist")

# DLJC will fail if these arguments are passed to it with empty values.
if [ ! "x${qual_classpath}" = "x" ]; then
  quals_arg='yes'
else
  quals_arg=
fi

if [ ! "x${checker_classpath}" = "x" ]; then
  lib_arg='yes'
else
  lib_arg=
fi

if [ ! "x${custom_stubs}" = "x" ]; then
  stubs_arg='yes'
fi

## Code starts here.

rm -rf "${checkername}-${repolistbase}-results"

bash wpi-many.sh -o "${workingdir}/${checkername}-${repolistbase}" \
     -i "${PARENTDIR}/${repolist}" \
     -t ${timeout} \
     -- \
     --checker "${checker}" \
     ${quals_arg:+--quals "${qual_classpath}"} \
     ${lib_arg:+--lib "${checker_classpath}"} \
     ${stubs_arg:+--stubs "${custom_stubs}"}
