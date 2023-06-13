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

# Initial commit is June 2018
echo "plume-scripts"
PLUME_SCRIPTS="checker/bin-devel/.plume-scripts"
if [ ! -d "$PLUME_SCRIPTS" ] ; then
  git clone -q https://github.com/plume-lib/plume-scripts.git "${PLUME_SCRIPTS}"
fi
COMMIT="$(cd "${PLUME_SCRIPTS}" && git rev-list -n 1 --first-parent --before="${commit_date}" master)"
if [ -n "${COMMIT}" ] ; then
  # COMMIT is non-empty
  (cd "${PLUME_SCRIPTS}" && git checkout -B __merge_eval__ "${COMMIT}")
fi

# Initial commit is February 2018
echo "html-tools"
HTML_TOOLS="checker/bin-devel/.plume-scripts"
COMMIT="$(cd "${HTML_TOOLS}" && git rev-list -n 1 --first-parent --before="${commit_date}" master)"
if [ ! -d "$HTML_TOOLS" ] ; then
  git clone -q https://github.com/plume-lib/html-tools.git "${HTML_TOOLS}"
fi
if [ -n "${COMMIT}" ] ; then
  # COMMIT is non-empty
  (cd "${HTML_TOOLS}" && git checkout -B __merge_eval__ "${COMMIT}")
fi

echo "Stubparser"
STUBPARSER="../stubparser"
if [ ! -d "${STUBPARSER}" ] ; then
  git clone https://github.com/typetools/stubparser.git "${STUBPARSER}"
fi
(cd "${STUBPARSER}" && git checkout -B __merge_eval__ "$(git rev-list -n 1 --first-parent --before="${commit_date}" master)")
if [ -f ${STUBPARSER}/.build-without-test.sh ] ; then
  STUBPARSER_BUILD=.build-without-test.sh
elif [ -f ${STUBPARSER}/.travis-build-without-test.sh ] ; then
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
if [ ! -d "${AT}" ] ; then
  git clone https://github.com/typetools/annotation-tools.git "${AT}"
fi
(cd "${AT}" && git checkout -B __merge_eval__ "$(git rev-list -n 1 --first-parent --before="${commit_date}" master)")
if [ -f ${AT}/.build-without-test.sh ] ; then
  AT_BUILD=.build-without-test.sh
elif [ -f ${AT}/.travis-build-without-test.sh ] ; then
  AT_BUILD=.travis-build-without-test.sh
else
  echo "Can't find stubparser build script"
  exit 1
fi
echo "Running:  (cd ${AT} && ./${AT_BUILD})"
(cd "${AT}" && ./${AT_BUILD})
echo "... done: (cd ${AT} && ./${AT_BUILD})"

JDK_DIR="../jdk"
if [ ! -d "${JDK_DIR}" ] ; then
  git clone https://github.com/typetools/jdk.git $JDK_DIR
fi
(cd "${JDK_DIR}" && git checkout -B __merge_eval__ "$(git rev-list -n 1 --first-parent --before="${commit_date}" master)")

./gradlew assemble

echo Exiting checker/bin-devel/checkout-historical.sh in "$(pwd)"
