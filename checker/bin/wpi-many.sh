#!/bin/bash

# This script runs the Checker Framework's whole-program inference on each of a list of projects.

# For usage and requirements, see the "Whole-program inference"
# section of the Checker Framework manual:
# https://checkerframework.org/manual/#whole-program-inference

set -eo pipefail
# not set -u, because this script checks variables directly

DEBUG=0
# To enable debugging, uncomment the following line.
# DEBUG=1

while getopts "o:i:u:t:g:s" opt; do
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
    s) SKIP_OR_DELETE_UNUSABLE="skip"
       ;;
    \?) # the remainder of the arguments will be passed to DLJC directly
       ;;
  esac
done

# Make $@ be the arguments that should be passed to dljc.
shift $(( OPTIND - 1 ))

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
SCRIPTPATH="${SCRIPTDIR}/wpi-many.sh"

# Report line numbers when the script fails, from
# https://unix.stackexchange.com/a/522815 .
trap 'echo >&2 "Error - exited with status $? at line $LINENO of wpi-many.sh:";
         pr -tn ${SCRIPTPATH} | tail -n+$((LINENO - 3)) | head -n7' ERR

echo "Starting wpi-many.sh."

# check required arguments and environment variables:

if [ "x${JAVA_HOME}" = "x" ]; then
  has_java_home="no"
else
  has_java_home="yes"
fi

# testing for JAVA8_HOME, not an unintentional reference to JAVA_HOME
# shellcheck disable=SC2153
if [ "x${JAVA8_HOME}" = "x" ]; then
  has_java8="no"
else
  has_java8="yes"
fi

# testing for JAVA11_HOME, not an unintentional reference to JAVA_HOME
# shellcheck disable=SC2153
if [ "x${JAVA11_HOME}" = "x" ]; then
  has_java11="no"
else
  has_java11="yes"
fi

if [ "${has_java_home}" = "yes" ]; then
    java_version=$("${JAVA_HOME}"/bin/java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
    if [ "${has_java8}" = "no" ] && [ "${java_version}" = 8 ]; then
      export JAVA8_HOME="${JAVA_HOME}"
      has_java8="yes"
    fi
    if [ "${has_java11}" = "no" ] && [ "${java_version}" = 11 ]; then
      export JAVA11_HOME="${JAVA_HOME}"
      has_java11="yes"
    fi
fi

if [ "${has_java8}" = "yes" ] && [ ! -d "${JAVA8_HOME}" ]; then
    echo "JAVA8_HOME is set to a non-existent directory ${JAVA8_HOME}"
    exit 1
fi

if [ "${has_java11}" = "yes" ] && [ ! -d "${JAVA11_HOME}" ]; then
    echo "JAVA11_HOME is set to a non-existent directory ${JAVA11_HOME}"
    exit 1
fi

if [ "${has_java8}" = "no" ] && [ "${has_java11}" = "no" ]; then
    echo "No Java 8 or 11 JDKs found. At least one of JAVA_HOME, JAVA8_HOME, or JAVA11_HOME must be set."
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

if [ "x${SKIP_OR_DELETE_UNUSABLE}" = "x" ]; then
  SKIP_OR_DELETE_UNUSABLE="delete"
fi

JAVA_HOME_BACKUP="${JAVA_HOME}"
export JAVA_HOME="${JAVA11_HOME}"

### Script

echo "Finished configuring wpi-many.sh. Results will be placed in ${OUTDIR}-results/."

export PATH="${JAVA_HOME}/bin:${PATH}"

mkdir -p "${OUTDIR}"
mkdir -p "${OUTDIR}-results"

cd "${OUTDIR}" || exit 5

while IFS='' read -r line || [ "$line" ]
do
    REPOHASH=${line}

    REPO=$(echo "${REPOHASH}" | awk '{print $1}')
    HASH=$(echo "${REPOHASH}" | awk '{print $2}')

    REPO_NAME=$(echo "${REPO}" | cut -d / -f 5)
    REPO_NAME_HASH="${REPO_NAME}-${HASH}"

    if [ "$DEBUG" -eq "1" ]; then
        echo "REPOHASH=$REPOHASH"
        echo "REPO=$REPO"
        echo "HASH=$HASH"
        echo "REPO_NAME=$REPO_NAME"
        echo "REPO_NAME_HASH=$REPO_NAME_HASH"
    fi

    # Use repo name and hash, but not owner.  We want
    # repos that are different but have the same name to be treated
    # as different repos, but forks with the same content to be skipped.
    # TODO: consider just using hash, to skip hard forks?
    mkdir -p "./${REPO_NAME_HASH}" || (echo "command failed in $(pwd): mkdir -p ./${REPO_NAME_HASH}" && exit 5)

    cd "./${REPO_NAME_HASH}" || (echo "command failed in $(pwd): cd ./${REPO_NAME_HASH}" && exit 5)

    if [ ! -d "${REPO_NAME}" ]; then
        # see https://stackoverflow.com/questions/3489173/how-to-clone-git-repository-with-specific-revision-changeset
        # for the inspiration for this code
        mkdir "./${REPO_NAME}" || (echo "command failed in $(pwd): mkdir ./${REPO_NAME}" && exit 5)
        cd "./${REPO_NAME}" || (echo "command failed in $(pwd): cd ./${REPO_NAME}" && exit 5)
        git init
        git remote add origin "${REPO}"

        # The "GIT_TERMINAL_PROMPT=0" setting prevents git from prompting for
        # username/password if the repository no longer exists.
        GIT_TERMINAL_PROMPT=0 git fetch origin "${HASH}"

        git reset --hard FETCH_HEAD

        cd .. || exit 5
        # Skip the rest of the loop and move on to the next project
        # if the checkout isn't successful.
        if [ ! -d "${REPO_NAME}" ]; then
           continue
        fi
    else
        rm -rf -- "${REPO_NAME}/dljc-out"
    fi

    cd "./${REPO_NAME}" || (echo "command failed in $(pwd): cd ./${REPO_NAME}" && exit 5)

    git checkout "${HASH}"

    OWNER=$(echo "${REPO}" | cut -d / -f 4)

    if [ "${OWNER}" = "${GITHUB_USER}" ]; then
        ORIGIN=$(echo "${REPOHASH}" | awk '{print $3}')
        # The `unannotated` remote is just a convenience for data analysis.
        # Running this script twice in a row on projects whose owner
        # is the github user always causes a harmless error on this
        # line, because the `unannotated` remote is already set.
        git remote add unannotated "${ORIGIN}" &> /dev/null || true
    fi

    REPO_FULLPATH=$(pwd)

    cd "${OUTDIR}/${REPO_NAME_HASH}" || exit 5

    RESULT_LOG="${OUTDIR}-results/${REPO_NAME_HASH}-wpi.log"
    touch "${RESULT_LOG}"

    if [ -f "${REPO_FULLPATH}/.cannot-run-wpi" ]; then
      if [ "${SKIP_OR_DELETE_UNUSABLE}" = "skip" ]; then
        echo "Skipping ${REPO_NAME_HASH} because it has a .cannot-run-wpi file present, indicating that an earlier run of WPI failed. To try again, delete the .cannot-run-wpi file and re-run the script."
      fi
      # the repo will be deleted later if SKIP_OR_DELETE_UNUSABLE is "delete"
    else
      /bin/bash -x "${SCRIPTDIR}/wpi.sh" -d "${REPO_FULLPATH}" -t "${TIMEOUT}" -g "${GRADLECACHEDIR}" -- "$@" &> "${OUTDIR}-results/wpi-out"
    fi

    cd "${OUTDIR}" || exit 5

    if [ -f "${REPO_FULLPATH}/.cannot-run-wpi" ]; then
        # If the result is unusable (i.e. wpi cannot run),
        # we don't need it for data analysis and we can
        # delete it right away.
        if [ "${SKIP_OR_DELETE_UNUSABLE}" = "delete" ]; then
          echo "Deleting ${REPO_NAME_HASH} because WPI could not be run."
          rm -rf -- "./${REPO_NAME_HASH}"
        fi
    else
        cat "${REPO_FULLPATH}/dljc-out/wpi.log" >> "${RESULT_LOG}"
        TYPECHECK_FILE=${REPO_FULLPATH}/dljc-out/typecheck.out
        if [ -f "$TYPECHECK_FILE" ]; then
            cp -p "$TYPECHECK_FILE" "${OUTDIR}-results/${REPO_NAME_HASH}-typecheck.out"
        else
            echo "Could not find file $TYPECHECK_FILE"
            ls -l "${REPO_FULLPATH}/dljc-out"
            cat "${REPO_FULLPATH}"/dljc-out/*.log
            echo "Start of toplevel.log:"
            cat "${REPO_FULLPATH}"/dljc-out/toplevel.log
            echo "End of toplevel.log."
            echo "Start of wpi.log:"
            cat "${REPO_FULLPATH}"/dljc-out/wpi.log
            echo "End of wpi.log."
        fi
    fi

    cd "${OUTDIR}" || exit 5

done < "${INLIST}"

## This section is here rather than in wpi-summary.sh because counting lines can be moderately expensive.
## wpi-summary.sh is intended to be run while a human waits (unlike this script), so this script
## precomputes as much as it can, to make wpi-summary.sh faster.

# this command is allowed to fail, because if no projects returned results then none
# of these expressions will match, and we want to enter the special handling for that
# case that appears below
results_available=$(grep -vl -e "no build file found for" \
    -e "dljc could not run the Checker Framework" \
    -e "dljc could not run the build successfully" \
    -e "dljc timed out for" \
    "${OUTDIR}-results/"*.log || true)

echo "${results_available}" > "${OUTDIR}-results/results_available.txt"

if [ -z "${results_available}" ]; then
  echo "No results are available."
  echo "Log files:"
  ls "${OUTDIR}-results"/*.log
  echo "End of log files."
else
  if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    listpath=$(mktemp "/tmp/cloc-file-list-$(date +%Y%m%d-%H%M%S)-XXX.txt")
    # Compute lines of non-comment, non-blank Java code in the projects whose
    # results can be inspected by hand (that is, those that WPI succeeded on).
    # Don't match arguments like "-J--add-opens=jdk.compiler/com.sun.tools.java".
    # shellcheck disable=SC2046
    grep -oh "\S*\.java" $(cat "${OUTDIR}-results/results_available.txt") | grep -v "^-J" | sed "s/'//g" | sort | uniq > "${listpath}"

    if [ ! -s "${listpath}" ] ; then
        echo "${listpath} has size zero"
        ls -l "${listpath}"
        echo "results_available = ${results_available}"
        echo "---------------- start of ${OUTDIR}-results/results_available.txt ----------------"
        cat "${OUTDIR}-results/results_available.txt"
        echo "---------------- end of ${OUTDIR}-results/results_available.txt ----------------"
        exit 1
    fi

    mkdir -p "${SCRIPTDIR}/.scc"
    cd "${SCRIPTDIR}/.scc" || exit 5
    wget -nc "https://github.com/boyter/scc/releases/download/v2.13.0/scc-2.13.0-i386-unknown-linux.zip"
    unzip -o "scc-2.13.0-i386-unknown-linux.zip"

    # shellcheck disable=SC2046
    "${SCRIPTDIR}/.scc/scc" --output "${OUTDIR}-results/loc.txt" \
        $(< "${listpath}")

    rm -f "${listpath}"
  else
    echo "skipping computation of lines of code because the operating system is not linux: ${OSTYPE}}"
  fi
fi

export JAVA_HOME="${JAVA_HOME_BACKUP}"

echo "Exiting wpi-many.sh. Results were placed in ${OUTDIR}-results/."
