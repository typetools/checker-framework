#!/bin/sh

# When run in a Checker Framework checkout/clone, this clones the related
# repositories, then builds them and the Checker Framework.  It sets the related
# repositories to their last commit before the Checker Framework's clone.  This
# approximates the state of the repositories at that time.

# This is only an approximation because if the Checker Framework was on a
# branch, then the related repository might have also been on a branch that was
# subsequently merged (or squash-and-merged, so the commit no longer exists in
# the related repository).

# This script works at least through mid-April 2019.
# It does not work for mid-January 2019.

# # To verify that you can build an old version of the Checker Framework, do
# # preparation, then set two environment variables, and then run these
# # commands.  See below for preparation and examples of setting the environment
# # variables.
# git clone https://github.com/typetools/checker-framework.git ${CHECKERFRAMEWORK}
# cd ${CHECKERFRAMEWORK}
# git checkout ${CFCOMMIT}
# /tmp/test-historical/checker-framework/checker/bin-devel/checkout-historical.sh
#
# # Preparation (only needs to be done once every)
# mkdir -p /tmp/test-historical
# git clone https://github.com/typetools/checker-framework.git
#
# # January 2023
# export CHECKERFRAMEWORK=/tmp/test-historical/checker-framework-202301
# export CFCOMMIT=9d60936fcd81827f3761d0244014a6e419133b16
#
# # July 2022
# export CHECKERFRAMEWORK=/tmp/test-historical/checker-framework-202207
# export CFCOMMIT=c37aff5ef28569e5bdadf681c81210d084de24df
#
# # January 2022
# export CHECKERFRAMEWORK=/tmp/test-historical/checker-framework-202201
# export CFCOMMIT=24364449c1bac6cee1896759e1ab5fc87ad5a70d
#
# # January 2021
# export CHECKERFRAMEWORK=/tmp/test-historical/checker-framework-202101
# export CFCOMMIT=f3cc3d328a70ef8e834bf2693be6cbb6a94ece63
#
# # January 2020
# export CHECKERFRAMEWORK=/tmp/test-historical/checker-framework-202001
# export CFCOMMIT=b7d026e424df2a04f8b9275bc2792cb03991425d
#
# # October 2019
# export CHECKERFRAMEWORK=/tmp/test-historical/checker-framework-201910
# export CFCOMMIT=b6e7558f3f0b0cf996f00039ca98a8d1fa798896
#
# # July 2019; use JDK 8
# export CHECKERFRAMEWORK=/tmp/test-historical/checker-framework-201907
# export CFCOMMIT=5000c1ecb72581aeebd3c10a2851cf003eeb554c
#
# # April 2019; use JDK 8
# export CHECKERFRAMEWORK=/tmp/test-historical/checker-framework-201904
# export CFCOMMIT=cc3b007addee9e241e4ef560d009fd212c478819
#
# # January 2019; use JDK 8
# # This fails because it references deleted repository https://bitbucket.org/typetools/jsr308-langtools
# export CHECKERFRAMEWORK=/tmp/test-historical/checker-framework-201901
# export CFCOMMIT=b76bd9dcd5839285a4dd9fd6c2d769647357f288
#
# # January 2018; use JDK 8
# # This fails, because plume-scripts does not exist.
# export CHECKERFRAMEWORK=/tmp/test-historical/checker-framework-201801
# export CFCOMMIT=1f48ddb600620454731170eb2628e5f7efa93c3e

# Fail the whole script if any command fails
set -e

# DEBUG=0
# # To enable debugging, uncomment the following line.
# # DEBUG=1

echo "Entering checker/bin-devel/checkout-historical.sh in $(pwd)"

commit_sha=$(git rev-parse HEAD)
commit_date=$(git show -s --format=%ci)

echo "Commit ${commit_sha}, date ${commit_date}"

git checkout -B __merge_eval__

# Initial commit is September 2023
echo "git-scripts"
GIT_SCRIPTS="checker/bin-devel/.git-scripts"
if [ ! -d "$GIT_SCRIPTS" ]; then
  git clone -q https://github.com/plume-lib/git-scripts.git "${GIT_SCRIPTS}"
fi
COMMIT="$(cd "${GIT_SCRIPTS}" && git rev-list -n 1 --first-parent --before="${commit_date}" master)"
if [ -n "${COMMIT}" ]; then
  # COMMIT is non-empty
  (cd "${GIT_SCRIPTS}" && git checkout -B __merge_eval__ "${COMMIT}")
fi

# Initial commit is June 2018
echo "plume-scripts"
PLUME_SCRIPTS="checker/bin-devel/.plume-scripts"
if [ ! -d "$PLUME_SCRIPTS" ]; then
  git clone -q https://github.com/plume-lib/plume-scripts.git "${PLUME_SCRIPTS}"
fi
COMMIT="$(cd "${PLUME_SCRIPTS}" && git rev-list -n 1 --first-parent --before="${commit_date}" master)"
if [ -n "${COMMIT}" ]; then
  # COMMIT is non-empty
  (cd "${PLUME_SCRIPTS}" && git checkout -B __merge_eval__ "${COMMIT}")
fi

# Initial commit is February 2018
echo "html-tools"
HTML_TOOLS="checker/bin-devel/.plume-scripts"
COMMIT="$(cd "${HTML_TOOLS}" && git rev-list -n 1 --first-parent --before="${commit_date}" master)"
if [ ! -d "$HTML_TOOLS" ]; then
  git clone -q https://github.com/plume-lib/html-tools.git "${HTML_TOOLS}"
fi
if [ -n "${COMMIT}" ]; then
  # COMMIT is non-empty
  (cd "${HTML_TOOLS}" && git checkout -B __merge_eval__ "${COMMIT}")
fi

echo "Stubparser"
STUBPARSER="../stubparser"
if [ ! -d "${STUBPARSER}" ]; then
  git clone https://github.com/typetools/stubparser.git "${STUBPARSER}"
fi
(cd "${STUBPARSER}" && git checkout -B __merge_eval__ "$(git rev-list -n 1 --first-parent --before="${commit_date}" master)")
if [ -f ${STUBPARSER}/.build-without-test.sh ]; then
  STUBPARSER_BUILD=.build-without-test.sh
elif [ -f ${STUBPARSER}/.travis-build-without-test.sh ]; then
  STUBPARSER_BUILD=.travis-build-without-test.sh
else
  echo "Can't find stubparser build script"
  exit 1
fi
echo "Running:  (cd ../stubparser/ && ./${STUBPARSER_BUILD})"
(cd ../stubparser/ && ./"${STUBPARSER_BUILD}")
echo "... done: (cd ../stubparser/ && ./${STUBPARSER_BUILD})"

echo "Annotation File Utilities"
AT="../annotation-tools"
if [ ! -d "${AT}" ]; then
  git clone https://github.com/typetools/annotation-tools.git "${AT}"
fi
(cd "${AT}" && git checkout -B __merge_eval__ "$(git rev-list -n 1 --first-parent --before="${commit_date}" master)")
if [ -f ${AT}/.build-without-test.sh ]; then
  AT_BUILD=.build-without-test.sh
elif [ -f ${AT}/.travis-build-without-test.sh ]; then
  AT_BUILD=.travis-build-without-test.sh
else
  echo "Can't find stubparser build script"
  exit 1
fi
echo "Running:  (cd ${AT} && ./${AT_BUILD})"
(cd "${AT}" && ./${AT_BUILD})
echo "... done: (cd ${AT} && ./${AT_BUILD})"

JDK_DIR="../jdk"
if [ ! -d "${JDK_DIR}" ]; then
  git clone https://github.com/typetools/jdk.git $JDK_DIR
fi
(cd "${JDK_DIR}" && git checkout -B __merge_eval__ "$(git rev-list -n 1 --first-parent --before="${commit_date}" master)")

./gradlew assemble

echo Exiting checker/bin-devel/checkout-historical.sh in "$(pwd)"
