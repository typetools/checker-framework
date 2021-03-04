#!/bin/sh

# Run wpi.sh on plume-lib projects.
# For each project:
#  * clone it
#  * remove its annotations
#  * run WPI to infer annotations
#  * type-check the annotated version
#  * check that the output of type-checking is the same as the *.expected file in this directory


# wpi.sh may exit with non-zero status.
set +e

# set -o verbose
# set -o xtrace
export SHELLOPTS
# echo "SHELLOPTS=${SHELLOPTS}"

SCRIPTDIR="$(cd "$(dirname "$0")" && pwd)"
CHECKERFRAMEWORK="$(cd "$(dirname "$0")"/../../.. && pwd)"

TESTDIR="$CHECKERFRAMEWORK/checker/build/wpi-plumelib-tests"

# Takes two arguments, an input file (produced by compilation) and an output file.
# Copies the input to the output, removing parts that might differ from run to run.
clean_compile_output() {
    in="$1"
    out="$2"

    cp -f "$in" "$out" || exit 1

    # Remove "Running ..." line
    sed -i '/^Running /d' "$out"

    # Remove comments starting with "#" and blank lines
    sed -i '/^#/d' "$out"
    sed -i '/^$/d' "$out"

    # Remove uninteresting output
    sed -i '/^warning: \[path\] bad path element /d' "$out"

    # Remove directory names and line numbers
    sed -i 's/^[^ ]*\///' "$out"
    sed -i 's/:[0-9]+: /: /' "$out"
}

# Takes two arguments, the project name and the comma-separated list of checkers to run.
test_wpi_plume_lib() {
    project="$1"
    checkers="$2"

    rm -rf "$project"
    git clone -q --depth 1 "https://github.com/plume-lib/$project.git"

    cd "$project" || (echo "can't run: cd $project" && exit 1)

    java -cp "$CHECKERFRAMEWORK/checker/dist/checker.jar" org.checkerframework.framework.stub.RemoveAnnotationsForInference . || exit 1
    "$CHECKERFRAMEWORK/checker/bin/wpi.sh" -b "-PskipCheckerFramework" -- --checker "$checkers"

    EXPECTED_FILE="$SCRIPTDIR/$project.expected"
    ACTUAL_FILE="$TESTDIR/$project/dljc-out/typecheck.out"
    clean_compile_output "$EXPECTED_FILE" "expected.txt"
    clean_compile_output "$ACTUAL_FILE" "actual.txt"
    if ! cmp --quiet expected.txt actual.txt ; then
      echo "Comparing $EXPECTED_FILE $ACTUAL_FILE in $(pwd)"
      diff -u expected.txt actual.txt
      exit 1
    fi

    cd ..
}


mkdir -p "$TESTDIR"
cd "$TESTDIR" || (echo "can't do: cd $TESTDIR" && exit 1)

# Get the list of checkers from the project's build.gradle file
test_wpi_plume_lib bcel-util         "formatter,interning,lock,nullness,regex,signature"
test_wpi_plume_lib bibtex-clean      "formatter,index,interning,lock,nullness,regex,signature"
test_wpi_plume_lib html-pretty-print "formatter,index,interning,lock,nullness,regex,signature"
test_wpi_plume_lib icalavailable     "formatter,index,interning,lock,nullness,regex,signature,initializedfields"
test_wpi_plume_lib lookup            "formatter,index,interning,lock,nullness,regex,signature"

echo "exiting test-wpi-plumelib.sh"
