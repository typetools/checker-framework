#!/usr/bin/env bash

# Outputs a list of all type qualifiers currently supported by the CF, in fully-qualified form.
# For a list that is not fully-qualified, run:
#   list-type-qualifiers.sh | sed 's/.*\.//' | sort | uniq

# TODO:
# This script ought to also output:
#   Nested annotations (defined as a nested class).
#   Aliased annotations, such as those in variable NONNULL_ALIASES.
#   (A few instances of these are hacked around with a print statement below.)
# It would be nice to have a command-line argument that controls whether the output is fully-qualified.

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

if [ "${CHECKERFRAMEWORK}x" = x ] ; then
    echo "CHECKERFRAMEWORK environment variable must be set to run this script. Run the following command to use this copy of the Checker Framework:
export CHECKERFRAMEWORK=${SCRIPT_DIR}/../../"
    exit 1
fi

(cd "${CHECKERFRAMEWORK}" &&
find \
     checker-qual/src/main/java \
     checker/src/test \
     docs/examples/units-extension \
     framework/src/main/java \
     framework/src/test/java \
     -name '*.java' -print0 \
    | xargs -0 grep --recursive --files-with-matches -e '^@Target\b.*TYPE_USE' \
    | sed 's/\.java$//' | sed 's/.*\/java\///' | sed 's/.*\/units-extension\///' \
    | awk '{print $1} END {print "NotNull.java"; print "UbTop.java"; print "LbTop.java"; print "UB_TOP.java"; print "LB_TOP.java";}' \
    | sed 's/\.java$//' | sed 's/\//./g' | sort | uniq \
)
