#!/bin/bash

# Run wpi.sh on plume-lib projects and check

set -e
# set -o verbose
# set -o xtrace
export SHELLOPTS
# echo "SHELLOPTS=${SHELLOPTS}"

# This does not use wpi-many.sh because:
#  * different projects use a different list of type-checkers.
#    (wpi.sh uses a fixed set of type-checkers for all projects)
#  * this uses the HEAD commit
#    (wpi.sh uses an old commit)
#  * this checks for expected errors
#    (wpi.sh requires that there are no errors)

SCRIPTDIR="$(cd "$(dirname "$0")" && pwd)"
CHECKERFRAMEWORK="$(cd "$(dirname "$0")"/../../.. && pwd)"

TESTDIR="$CHECKERFRAMEWORK/checker/build/wpi-plumelib-tests"

# Takes two arguments, an input file (produced by compilation) and an output file.
# Copies the input to the output, removing parts that might differ from run to run.
clean_compile_output() {
    in="$1"
    out="$2"
    rm -rf "$out"
    cp "$in" "$out"

    # Remove "Running ..." line
    sed -i '/^Running /d' "$out"

    # Remove comments starting with "#" and blank lines
    sed -i '/^#/d' "$out"
    sed -i '/^$/d' "$out"

    # Remove uninteresting output
    sed -i '/^warning: \[path\] bad path element /d' "$out"

    # Remove directory names and line numbers
    sed -i 's/.*\///' "$out"
    sed -i 's/:[0-9]+: /: /' "$out"
}

# Takes two arguments, the project name and the comma-separated list of checkers to run.
test_wpi_plume_lib() {
    project="$1"
    checkers="$2"

    rm -rf "$project"
    git clone -q --depth 1 "https://github.com/plume-lib/$project.git"

    cd "$project"

    "$CHECKERFRAMEWORK/checker/bin-devel/remove-annotations.sh"
    "$CHECKERFRAMEWORK/checker/bin/wpi.sh" -b "-PskipCheckerFramework" -- --checker "$checkers"

    EXPECTED_FILE="$SCRIPTDIR/$project.expected"
    ACTUAL_FILE="$TESTDIR/$project/dljc-out/typecheck.out"
    echo "Comparing $EXPECTED_FILE $ACTUAL_FILE"
    clean_compile_output "$EXPECTED_FILE" "expected.txt"
    clean_compile_output "$ACTUAL_FILE" "actual.txt"
    diff -u expected.txt actual.txt

    cd ..
}


mkdir -p "$TESTDIR"
cd "$TESTDIR"

test_wpi_plume_lib bcel-util         "formatter,interning,lock,nullness,regex,signature"
test_wpi_plume_lib bibtex-clean      "formatter,index,interning,lock,nullness,regex,signature"
test_wpi_plume_lib html-pretty-print "formatter,index,interning,lock,nullness,regex,signature"
test_wpi_plume_lib icalavailable     "formatter,index,interning,lock,nullness,regex,signature,initializedfields"

echo "exiting test-wpi-plumelib.sh"
