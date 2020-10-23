#!/bin/bash

# This script runs the Checker Framework's whole-program inference on each of a list of projects.

### Usage
#
# See the README.md file that accompanies this script.
#

### Dependencies:
#
# - JAVA8_HOME environment variable must point to a Java 8 JDK
# - JAVA11_HOME environment variable must point to a Java 11 JDK
# - CHECKERFRAMEWORK environment variable must point to a built copy of the Checker Framework
# - Other dependencies: python2.7 (for dljc), awk, git, mvn, gradle, wget, curl
#

### Required arguments:
#
# -o outdir : run the experiment in the ./outdir directory, and place the
#             results in the ./outdir-results directory. Both will be created
#             if they do not exist.
#
# -i infile : read the list of repositories to use from the file $infile. Each
#             line should have 2 or 3 elements:
#              * The URL of the git repository on GitHub. The URL must be of the
#                form:  https://github.com/username/repository .  The script is
#                reliant on the number of slashes, so excluding https:// is an
#                error.
#              * The commit hash to use, separated by whitespace.
#              * if the repository's owner is the user specified by
#                the -u flag, the original (upstream) GitHub
#                repository.  Its only use is to be made an upstream
#                named "unannotated".


### Optional arguments:
#
# -u user : the GitHub owner for repositories that have
#           been forked and modified. These repositories must have a third entry
#           in the infile indicating their origin. Default is "$USER".
#
# -t timeout : the timeout for running the checker (via WPI) on a single project, in seconds
#
# Any arguments that follow these arguments (and are separated by a
# literal "--") are passed directly to DLJC without modification. See
# the help text of DLJC (e.g. clone
# https://github.com/kelloggm/do-like-javac and run
# `python2 dljc --help`) for an explanation of these arguments.
#
# At least one such argument is required: --checker, which tells DLJC
# what typechecker to run. A literal "--" argument must be given
# before the DLJC arguments (if there are any) to indicate that the
# remaining arguments should be passed to DLJC rather than interpreted
# as command line options for this script.
#

while getopts "o:i:u:t:" opt; do
  case $opt in
    o) OUTDIR="$OPTARG"
       ;;
    i) INLIST="$OPTARG"
       ;;
    u) GITHUB_USER="$OPTARG"
       ;;
    t) TIMEOUT="$OPTARG"
       ;;
    \?) # the remainder of the arguments will be passed to DLJC directly
       ;;
  esac
done

# Make $@ be the arguments that should be passed to dljc.
shift $(( OPTIND - 1 ))

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

export JAVA_HOME="${JAVA11_HOME}"

### Script

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

    "${SCRIPTDIR}/wpi.sh" -d "${REPO_FULLPATH}" -u "${GITHUB_USER}" -t "${TIMEOUT}" -- "$@" &> "${RESULT_LOG}"

    popd || exit 5

    # If the result is unusable (i.e. wpi cannot run),
    # we don't need it for data analysis and we can
    # delete it right away.
    if [ -f "${REPO_FULLPATH}/.cannot-run-wpi" ]; then
        rm -rf "${REPO_NAME_HASH}" &
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

    pushd "${SCRIPTDIR}/.." || exit 5
    wget -nc "https://github.com/boyter/scc/releases/download/v2.13.0/scc-2.13.0-i386-unknown-linux.zip"
    unzip -o "scc-2.13.0-i386-unknown-linux.zip"
    popd || exit 5

    "${SCRIPTDIR}/../scc" --output "${OUTDIR}-results/loc.txt" \
        "$(< "${listpath}")"

    rm -f "${listpath}"
fi
