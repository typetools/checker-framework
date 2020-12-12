#!/bin/bash

# This script runs the Checker Framework's whole-program inference on each of a list of projects.

# For usage and requirements, see the "Whole-program inference"
# section of the Checker Framework manual:
# https://checkerframework.org/manual/#whole-program-inference

while getopts "o:i:u:t:g:" opt; do
  case $opt in
    o) OUTDIR="$OPTARG"
       ;;
    i) INLIST="$OPTARG"
       ;;
    u) GITHUB_USER="$OPTARG"
       ;;
    t) TIMEOUT="$OPTARG"
       ;;
    g) GRADLECACHEDIR="$OPTARG"
       ;;
    \?) # the remainder of the arguments will be passed to DLJC directly
       ;;
  esac
done

# Make $@ be the arguments that should be passed to dljc.
shift $(( OPTIND - 1 ))

echo "Starting wpi-many.sh. The output of this script is purely informational."

# check required arguments and environment variables:

# testing for JAVA8_HOME, not an unintentional reference to JAVA_HOME
# shellcheck disable=SC2153
if [ "x${JAVA8_HOME}" = "x" ]; then
    echo "JAVA8_HOME must be set to a Java 8 JDK"
    exit 1
fi

if [ ! -d "${JAVA8_HOME}" ]; then
    echo "JAVA8_HOME is set to a non-existent directory ${JAVA8_HOME}"
    exit 1
fi

# testing for JAVA11_HOME, not an unintentional reference to JAVA_HOME
# shellcheck disable=SC2153
if [ "x${JAVA11_HOME}" = "x" ]; then
    echo "JAVA11_HOME must be set to a Java 11 JDK"
    exit 1
fi

if [ ! -d "${JAVA11_HOME}" ]; then
    echo "JAVA11_HOME is set to a non-existent directory ${JAVA11_HOME}"
    exit 1
fi

if [ "x${CHECKERFRAMEWORK}" = "x" ]; then
    echo "CHECKERFRAMEWORK is not set; it must be set to a locally-built Checker Framework. Please clone and build github.com/typetools/checker-framework"
    exit 2
fi

if [ ! -d "${CHECKERFRAMEWORK}" ]; then
    echo "CHECKERFRAMEWORK is set to a non-existent directory ${CHECKERFRAMEWORK}"
    exit 2
fi

if [ "x${OUTDIR}" = "x" ]; then
    echo "You must specify an output directory using the -o argument."
    exit 3
fi

if [ "x${INLIST}" = "x" ]; then
    echo "You must specify an input file using the -i argument."
    exit 4
fi

if [ "x${GITHUB_USER}" = "x" ]; then
    GITHUB_USER="${USER}"
fi

if [ "x${GRADLECACHEDIR}" = "x" ]; then
  GRADLECACHEDIR=".gradle"
fi

JAVA_HOME_BACKUP="${JAVA_HOME}"
export JAVA_HOME="${JAVA11_HOME}"

### Script

echo "Finished configuring wpi-many.sh. Results will be placed in ${OUTDIR}-results/."

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

export PATH="${JAVA_HOME}/bin:${PATH}"

mkdir -p "${OUTDIR}"
mkdir -p "${OUTDIR}-results"

pushd "${OUTDIR}" || exit 5

while IFS='' read -r line || [ "$line" ]
do
    REPOHASH=${line}

    REPO=$(echo "${REPOHASH}" | awk '{print $1}')
    HASH=$(echo "${REPOHASH}" | awk '{print $2}')

    REPO_NAME=$(echo "${REPO}" | cut -d / -f 5)
    REPO_NAME_HASH="${REPO_NAME}-${HASH}"

    # Use repo name and hash, but not owner.  We want
    # repos that are different but have the same name to be treated
    # as different repos, but forks with the same content to be skipped.
    # TODO: consider just using hash, to skip hard forks?
    mkdir -p "${REPO_NAME_HASH}"

    pushd "${REPO_NAME_HASH}" || exit 5

    if [ ! -d "${REPO_NAME}" ]; then
        # The "GIT_TERMINAL_PROMPT=0" setting prevents git from prompting for
        # username/password if the repository no longer exists.
        GIT_TERMINAL_PROMPT=0 git clone "${REPO}"
        # Skip the rest of the loop and move on to the next project
        # if cloning isn't successful.
        if [ ! -d "${REPO_NAME}" ]; then
           continue
        fi
    else
        rm -rf "${REPO_NAME}/dljc-out"
    fi

    pushd "${REPO_NAME}" || exit 5

    git checkout "${HASH}"

    OWNER=$(echo "${REPO}" | cut -d / -f 4)

    if [ "${OWNER}" = "${GITHUB_USER}" ]; then
        ORIGIN=$(echo "${REPOHASH}" | awk '{print $3}')
        git remote add unannotated "${ORIGIN}"
    fi

    REPO_FULLPATH=$(pwd)

    popd || exit 5

    RESULT_LOG="${OUTDIR}-results/${REPO_NAME_HASH}-wpi.log"
    touch "${RESULT_LOG}"

    "${SCRIPTDIR}/wpi.sh" -d "${REPO_FULLPATH}" -t "${TIMEOUT}" -g "${GRADLECACHEDIR}" -- "$@" &> "${RESULT_LOG}"

    popd || exit 5

    # If the result is unusable (i.e. wpi cannot run),
    # we don't need it for data analysis and we can
    # delete it right away.
    if [ -f "${REPO_FULLPATH}/.cannot-run-wpi" ]; then
        # rm -rf "${REPO_NAME_HASH}" &
        echo "I love deleting things"
    else
        cat "${REPO_FULLPATH}/dljc-out/wpi.log" >> "${RESULT_LOG}"
    fi

    cd "${OUTDIR}" || exit 5

done <"${INLIST}"

popd || exit 5

## This section is here rather than in wpi-summary.sh because cloc can be moderately expensive.
## wpi-summary.sh is intended to be run while a human waits (unlike this script), so this script
## precomputes as much as it can, to make wpi-summary.sh faster.

results_available=$(grep -Zvl "no build file found for" "${OUTDIR}-results/"*.log \
    | xargs -0 grep -Zvl "dljc could not run the Checker Framework" \
    | xargs -0 grep -Zvl "dljc could not run the build successfully" \
    | xargs -0 grep -Zvl "dljc timed out for" \
    | xargs -0 echo)

echo "${results_available}" > "${OUTDIR}-results/results_available.txt"

if [ -n "${results_available}" ]; then
    listpath=$(mktemp /tmp/cloc-file-list-XXX.txt)
    # Compute lines of non-comment, non-blank Java code in the projects whose
    # results can be inspected by hand (that is, those that WPI succeeded on).
    grep -oh "\S*\.java" "${results_available}" | sort | uniq > "${listpath}"

    pushd "${SCRIPTDIR}/.do-like-javac" || exit 5
    wget -nc "https://github.com/boyter/scc/releases/download/v2.13.0/scc-2.13.0-i386-unknown-linux.zip"
    unzip -o "scc-2.13.0-i386-unknown-linux.zip"
    popd || exit 5

    "${SCRIPTDIR}/.do-like-javac/scc" --output "${OUTDIR}-results/loc.txt" \
        "$(< "${listpath}")"

    rm -f "${listpath}"
fi

export JAVA_HOME="${JAVA_HOME_BACKUP}"

echo "Exiting wpi-many.sh. The output of this script was purely informational. Results were placed in ${OUTDIR}-results/."
