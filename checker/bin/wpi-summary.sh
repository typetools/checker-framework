#!/bin/sh

# This script takes a directory of .log files as input, and produces a summary of the results.
# Use its output to guide your analysis of the results of running ./wpi-many.sh.
#
# This script categorizes projects as:
#  * does not have a maven or gradle build file
#  * failed to build
#  * WPI timed out
#  * WPI produced results (reported as "results available"). This
#    script makes no attempt to categorize these projects: a human
#    should inspect these log files/projects to see the results.

targetdir=$1

number_of_projects=$(find "${targetdir}" -name "*.log" | wc -l)

no_build_file=$(grep -cl "no build file found for" "${targetdir}/"*.log)
no_build_file_percent=$(((no_build_file*100)/number_of_projects))

# "old" and "new" in the below refer to the two different messages that
# dljc's wpi tool can emit for this kind of failure. At some point while
# running an early set of these experiments, I realized that the original
# message wasn't correct, and fixed it. But, for backwards compatibility,
# this script looks for both messages and combines the counts.
build_failed_old=$(grep -cl "dljc could not run the Checker Framework" "${targetdir}/"*.log)
build_failed_new=$(grep -cl "dljc could not run the build successfully" "${targetdir}/"*.log)
build_failed=$((build_failed_old+build_failed_new))
build_failed_percent=$(((build_failed*100)/number_of_projects))

timed_out=$(grep -cl "dljc timed out for" "${targetdir}/"*.log)
timed_out_percent=$(((timed_out*100)/number_of_projects))

echo "number of projects: ${number_of_projects} (100%)"
echo "no maven or gradle build file: ${no_build_file} (~${no_build_file_percent}%)"
echo "build failed: ${build_failed} (~${build_failed_percent}%)"
echo "timed out: ${timed_out} (~${timed_out_percent}%)"
echo ""
echo "timeouts:"
echo ""
grep -l "dljc timed out for" "${targetdir}/"*.log
echo ""

results_available=$(cat "${targetdir}/results_available.txt")

echo "results are available for these projects: "
echo ""
echo "${results_available}" | tr ' ' '\n'
echo ""

if [ -f "${targetdir}/loc.txt" ]; then
    echo "LoC of projects with available results:"

    cat "${targetdir}/loc.txt"
else
    echo "No LoC count found for projects with available results"
fi
