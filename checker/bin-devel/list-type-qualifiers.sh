#!/usr/bin/env bash

# outputs a list of all type qualifiers currently supported by the CF to type-qualifiers.txt

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

if [ "${CHECKERFRAMEWORK}x" = x ] ; then
    echo "CHECKERFRAMEWORK environment variable must be set to run this script. Run the following command to use this copy of the Checker Framework:
export CHECKERFRAMEWORK=${SCRIPT_DIR}/../../"
    exit 1
fi

grep --recursive --files-with-matches -e '^@Target\b.*TYPE_USE' \
     "${CHECKERFRAMEWORK}/checker/src/test" \
     "${CHECKERFRAMEWORK}/checker-qual/src/main/java" \
     "${CHECKERFRAMEWORK}/framework/src/main/java" \
     "${CHECKERFRAMEWORK}/docs/examples/units-extension" \
     "${CHECKERFRAMEWORK}/framework/src/test/java" \
    | grep -v '~' | sed 's/.*\///' \
    | awk '{print $1} END {print "NotNull.java"; print "UbTop.java"; print "LbTop.java"; print "UB_TOP.java"; print "LB_TOP.java";}' \
    | sed 's/\(.*\)\.java/        "\1",/' | sort | uniq > type-qualifiers.txt
