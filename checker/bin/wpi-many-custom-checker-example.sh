#!/bin/sh

## This is an example script that runs wpi-many.sh with appropriate
## arguments and environment variables, using a custom typechecker.
## If you want to use this script, you'll have to change
## environment variables and paths below.  Note that this script is
## intended for use with a custom typechecker (i.e., a typechecker
## that is not in the main Checker Framework distribution).  If your
## typechecker is in the main Checker Framework distribution, you
## should use wpi-many.sh or wpi.sh directly rather than making a copy
## of this script.

## Change these if necessary.

export JAVA11_HOME=/usr/lib/jvm/java-11-openjdk/
export JAVA8_HOME=/usr/lib/jvm/java-1.8.0-openjdk

## Everyone must change these.

export PARENTDIR=${HOME}/compliance-experiments/fse20
export CHECKERFRAMEWORK=${PARENTDIR}/checker-framework
checker=org.checkerframework.checker.noliteral.NoLiteralChecker
checkername=no-literal
repolist=securerandom.list
workingdir=$(pwd)

# the stub files for the checker being used
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
checker_classpath='/homes/gws/kelloggm/compliance-experiments/fse20/no-literal-checker/no-literal-checker/build/classes/java/main:/homes/gws/kelloggm/compliance-experiments/fse20/no-literal-checker/no-literal-checker/build/resources/main:/homes/gws/kelloggm/compliance-experiments/fse20/checker-framework/checker/dist/checker.jar:/homes/gws/kelloggm/compliance-experiments/fse20/no-literal-checker/no-literal-qual/build/libs/no-literal-qual.jar:/homes/gws/kelloggm/.gradle/caches/modules-2/files-2.1/com.google.errorprone/javac/9+181-r4173-1/bdf4c0aa7d540ee1f7bf14d47447aea4bbf450c5/javac-9+181-r4173-1.jar:/homes/gws/kelloggm/.gradle/caches/modules-2/files-2.1/org.checkerframework/checker-qual/3.1.1/361404eff7f971a296020d47c928905b3b9c5b5f/checker-qual-3.1.1.jar'

# The qualifier classpath. Usually, this is a subset of
# checker_classpath that contains just two elements:
#  * the qual jar for your checker, and
#  * the version of checker-qual.jar that your qualifiers depend on.
#
# Like checker_classpath, this is usually generated using the printClasspath
# task in the qualifier subproject of your custom checker, if it has one.
#
qual_classpath='/homes/gws/kelloggm/compliance-experiments/fse20/no-literal-checker/no-literal-qual/build/libs/no-literal-qual.jar:/homes/gws/kelloggm/.gradle/caches/modules-2/files-2.1/org.checkerframework/checker-qual/3.1.1/361404eff7f971a296020d47c928905b3b9c5b5f/checker-qual-3.1.1.jar'



## Optionally change these.

export ANDROID_HOME=${PARENTDIR}/android_home
timeout=3600 # 60 minutes

## There is no need to make changes below this point.

export JAVA_HOME=${JAVA11_HOME}
repolistbase=$(basename "$repolist")

## Code starts here.

rm -rf "${checkername}-${repolistbase}-results"

bash wpi-many.sh -o "${workingdir}/${checkername}-${repolistbase}" \
     -i "${PARENTDIR}/${repolist}" \
     -t ${timeout} \
     -- \
     --checker "${checker}" \
     --quals "${qual_classpath}" \
     --lib "${checker_classpath}" \
     --stubs "${custom_stubs}"
