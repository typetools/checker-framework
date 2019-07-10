#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS

git -C /tmp/plume-scripts pull > /dev/null 2>&1 \
  || git -C /tmp clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git
eval `/tmp/plume-scripts/ci-info typetools`

export CHECKERFRAMEWORK=`readlink -f ${CHECKERFRAMEWORK:-.}`
echo "CHECKERFRAMEWORK=$CHECKERFRAMEWORK"

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source $SCRIPTDIR/build.sh ${BUILDJDK}


# Code style and formatting
./gradlew checkBasicStyle checkFormat --console=plain --warning-mode=all --no-daemon

# Run error-prone
./gradlew runErrorProne --console=plain --warning-mode=all --no-daemon

# HTML legality
./gradlew htmlValidate --console=plain --warning-mode=all --no-daemon

# Documentation
./gradlew javadocPrivate --console=plain --warning-mode=all --no-daemon
make -C docs/manual all

# This comes last, in case we wish to ignore it
# if [ "$CI_IS_PR" == "true" ] ; then
(git diff $CI_COMMIT_RANGE > /tmp/diff.txt 2>&1) || true
(./gradlew requireJavadocPrivate --console=plain --warning-mode=all --no-daemon > /tmp/warnings.txt 2>&1) || true
[ -s /tmp/diff.txt ] || (echo "/tmp/diff.txt is empty for $CI_COMMIT_RANGE; try pulling base branch (often master) into compare branch (often your feature branch)" && false)
python /tmp/plume-scripts/lint-diff.py --guess-strip /tmp/diff.txt /tmp/warnings.txt
