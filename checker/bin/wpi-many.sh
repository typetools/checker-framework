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

while getopts "o:i:t:g:s" opt; do
  case $opt in
    o) OUTDIR="$OPTARG"
       ;;
    i) INLIST="$OPTARG"
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

# shellcheck disable=SC2153 # testing for JAVA_HOME, not a typo of JAVA8_HOME
if [ "${JAVA_HOME}" = "" ]; then
  has_java_home="no"
else
  has_java_home="yes"
fi

# shellcheck disable=SC2153 # testing for JAVA8_HOME, not a typo of JAVA_HOME
if [ "${JAVA8_HOME}" = "" ]; then
  has_java8="no"
else
  has_java8="yes"
fi

# shellcheck disable=SC2153 # testing for JAVA11_HOME, not a typo of JAVA_HOME
if [ "${JAVA11_HOME}" = "" ]; then
  has_java11="no"
else
  has_java11="yes"
fi

# shellcheck disable=SC2153 # testing for JAVA17_HOME, not a typo of JAVA_HOME
if [ "${JAVA17_HOME}" = "" ]; then
  has_java17="no"
else
  has_java17="yes"
fi

# shellcheck disable=SC2153 # testing for JAVA20_HOME, not a typo of JAVA_HOME
if [ "${JAVA20_HOME}" = "" ]; then
  has_java20="no"
else
  has_java20="yes"
fi

# shellcheck disable=SC2153 # testing for JAVA21_HOME, not a typo of JAVA_HOME
if [ "${JAVA21_HOME}" = "" ]; then
  has_java21="no"
else
  has_java21="yes"
fi

if [ "${has_java_home}" = "yes" ] && [ ! -d "${JAVA_HOME}" ]; then
    echo "JAVA_HOME is set to a non-existent directory ${JAVA_HOME}"
    exit 1
fi

if [ "${has_java_home}" = "yes" ]; then
    java_version=$("${JAVA_HOME}"/bin/java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//')
    if [ "${has_java8}" = "no" ] && [ "${java_version}" = 8 ]; then
      export JAVA8_HOME="${JAVA_HOME}"
      has_java8="yes"
    fi
    if [ "${has_java11}" = "no" ] && [ "${java_version}" = 11 ]; then
      export JAVA11_HOME="${JAVA_HOME}"
      has_java11="yes"
    fi
    if [ "${has_java17}" = "no" ] && [ "${java_version}" = 17 ]; then
      export JAVA17_HOME="${JAVA_HOME}"
      has_java17="yes"
    fi
    if [ "${has_java20}" = "no" ] && [ "${java_version}" = 20 ]; then
      export JAVA20_HOME="${JAVA_HOME}"
      has_java20="yes"
    fi
    if [ "${has_java21}" = "no" ] && [ "${java_version}" = 21 ]; then
      export JAVA21_HOME="${JAVA_HOME}"
      has_java21="yes"
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

if [ "${has_java17}" = "yes" ] && [ ! -d "${JAVA17_HOME}" ]; then
    echo "JAVA17_HOME is set to a non-existent directory ${JAVA17_HOME}"
    exit 1
fi

if [ "${has_java20}" = "yes" ] && [ ! -d "${JAVA20_HOME}" ]; then
    echo "JAVA20_HOME is set to a non-existent directory ${JAVA20_HOME}"
    exit 1
fi

if [ "${has_java21}" = "yes" ] && [ ! -d "${JAVA21_HOME}" ]; then
    echo "JAVA21_HOME is set to a non-existent directory ${JAVA21_HOME}"
    exit 1
fi

if [ "${has_java8}" = "no" ] && [ "${has_java11}" = "no" ] && [ "${has_java17}" = "no" ] && [ "${has_java20}" = "no" ] && [ "${has_java21}" = "no" ]; then
    if [ "${has_java_home}" = "yes" ]; then
      echo "Cannot determine Java version from JAVA_HOME"
    else
      echo "No Java 8, 11, 17, 20, or 21 JDKs found. At least one of JAVA_HOME, JAVA8_HOME, JAVA11_HOME, JAVA17_HOME, or JAVA21_HOME must be set."
    fi
    echo "JAVA_HOME = ${JAVA_HOME}"
    echo "JAVA8_HOME = ${JAVA8_HOME}"
    echo "JAVA11_HOME = ${JAVA11_HOME}"
    echo "JAVA17_HOME = ${JAVA17_HOME}"
    echo "JAVA20_HOME = ${JAVA20_HOME}"
    echo "JAVA21_HOME = ${JAVA21_HOME}"
    command -v java
    java -version
    exit 1
fi

if [ "${CHECKERFRAMEWORK}" = "" ]; then
    echo "CHECKERFRAMEWORK is not set; it must be set to a locally-built Checker Framework. Please clone and build github.com/typetools/checker-framework"
    exit 2
fi

if [ ! -d "${CHECKERFRAMEWORK}" ]; then
    echo "CHECKERFRAMEWORK is set to a non-existent directory ${CHECKERFRAMEWORK}"
    exit 2
fi

if [ "${OUTDIR}" = "" ]; then
    echo "You must specify an output directory using the -o argument."
    exit 3
fi

if [ "${INLIST}" = "" ]; then
    echo "You must specify an input file using the -i argument."
    exit 4
fi

if [ "${GRADLECACHEDIR}" = "" ]; then
  # Assume that each project should use its own gradle cache. This is more expensive,
  # but prevents crashes on distributed file systems, such as the UW CSE machines.
  GRADLECACHEDIR=".gradle"
fi

if [ "${SKIP_OR_DELETE_UNUSABLE}" = "" ]; then
  SKIP_OR_DELETE_UNUSABLE="delete"
fi

### Script

echo "Finished configuring wpi-many.sh. Results will be placed in ${OUTDIR}-results/."

export PATH="${JAVA_HOME}/bin:${PATH}"

mkdir -p "${OUTDIR}"
mkdir -p "${OUTDIR}-results"

cd "${OUTDIR}" || exit 5

while IFS='' read -r line || [ "$line" ]
do
    # Skip lines that start with "#".
    [[ $line = \#* ]] && continue

    # Remove trailing return character if reading from a DOS file.
    line="$(echo "$line" | tr -d '\r')"

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
        echo "pwd=$(pwd)"
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

    if [ ! -d "${REPO_NAME}/.git" ]; then
        echo "In $(pwd): no directory ${REPO_NAME}/.git"
        echo "Listing of ${REPO_NAME}:"
        ls -al -- "${REPO_NAME}"
        exit 5
    fi

    cd "./${REPO_NAME}" || (echo "command failed in $(pwd): cd ./${REPO_NAME}" && exit 5)

    git checkout "${HASH}" || (echo "command failed in $(pwd): git checkout ${HASH}" && exit 5)

    REPO_FULLPATH=$(pwd)

    cd "${OUTDIR}/${REPO_NAME_HASH}" || exit 5

    RESULT_LOG="${OUTDIR}-results/${REPO_NAME_HASH}-wpi-stdout.log"
    touch "${RESULT_LOG}"

    if [ -f "${REPO_FULLPATH}/.cannot-run-wpi" ]; then
      if [ "${SKIP_OR_DELETE_UNUSABLE}" = "skip" ]; then
        echo "Skipping ${REPO_NAME_HASH} because it has a .cannot-run-wpi file present,"
	echo "indicating that an earlier run of WPI failed."
	echo "To try again, delete the .cannot-run-wpi file and re-run the script."
      fi
      # the repo will be deleted later if SKIP_OR_DELETE_UNUSABLE is "delete"
    else
      # it's important that </dev/null is on this line, or wpi.sh might consume stdin, which would stop the larger wpi-many loop early
      echo "wpi-many.sh about to call wpi.sh in $(pwd) at $(date)"
      /bin/bash -x "${SCRIPTDIR}/wpi.sh" -d "${REPO_FULLPATH}" -t "${TIMEOUT}" -g "${GRADLECACHEDIR}" -- "$@" &> "${OUTDIR}-results/wpi-out" </dev/null
      wpi_status=$?
      if [[ $wpi_status -eq 0 ]]; then
        wpi_status_string="success"
      else
        wpi_status_string="failure"
      fi
      echo "wpi-many.sh finished call to wpi.sh with status ${wpi_status} (${wpi_status_string}) in $(pwd) at $(date)"
      # The test of $wpi_status below may halt wpi-many.sh.
      if [ "$DEBUG" -eq "1" ]; then
          echo "Listing of $(pwd):"
          ls -al "$(pwd)"
          echo "Listing of ${REPO_FULLPATH}:"
          ls -al "${REPO_FULLPATH}"
          echo "Listing of ${REPO_FULLPATH}/dljc-out:"
          ls -al "${REPO_FULLPATH}/dljc-out"
      fi
    fi

    cd "${OUTDIR}" || exit 5

    if [ -f "${REPO_FULLPATH}/.cannot-run-wpi" ]; then
        echo "Cannot run WPI: file ${REPO_FULLPATH}/.cannot-run-wpi exists."
        cat "${REPO_FULLPATH}/.cannot-run-wpi"
        echo "Listing of $(pwd):"
        ls -al "$(pwd)"
        echo "Listing of ${REPO_FULLPATH}:"
        ls -al "${REPO_FULLPATH}"
        if [ ! -d "${REPO_FULLPATH}/dljc-out" ] ; then
            echo "Does not exist: ${REPO_FULLPATH}/dljc-out"
        else
            echo "Listing of ${REPO_FULLPATH}/dljc-out:"
            ls -al "${REPO_FULLPATH}"/dljc-out
            for f in "${REPO_FULLPATH}"/dljc-out/* ; do
                echo "==== start of tail of ${f} ===="
                tail -n 2000 "${f}"
                sleep 1
                echo "==== end of tail of ${f} ===="
            done
        fi

        # If the result is unusable (i.e. wpi cannot run),
        # we don't need it for data analysis and we can
        # delete it right away.
        if [ "${SKIP_OR_DELETE_UNUSABLE}" = "delete" ]; then
          echo "Deleting ${REPO_NAME_HASH} because WPI could not be run."
          rm -rf -- "./${REPO_NAME_HASH}"
        fi
    else
        cat "${REPO_FULLPATH}/dljc-out/wpi-stdout.log" >> "${RESULT_LOG}"
        if [ ! -s "${RESULT_LOG}" ] ; then
          echo "Files are empty: ${REPO_FULLPATH}/dljc-out/wpi-stdout.log ${RESULT_LOG}"
          echo "Listing of ${REPO_FULLPATH}/dljc-out:"
          ls -al "${REPO_FULLPATH}/dljc-out"
          wpi_status=9999
        fi
        TYPECHECK_FILE=${REPO_FULLPATH}/dljc-out/typecheck.out
        if [ -f "$TYPECHECK_FILE" ]; then
            cp -p "$TYPECHECK_FILE" "${OUTDIR}-results/${REPO_NAME_HASH}-typecheck.out"
            if [ "$DEBUG" -eq "1" ]; then
                echo "File exists: $TYPECHECK_FILE"
                echo "File exists: ${OUTDIR}-results/${REPO_NAME_HASH}-typecheck.out"
            fi
        else
            echo "File does not exist: $TYPECHECK_FILE"
            echo "File does not exist: ${OUTDIR}-results/${REPO_NAME_HASH}-typecheck.out"
            echo "Listing of ${REPO_FULLPATH}/dljc-out:"
            ls -al "${REPO_FULLPATH}/dljc-out"
            cat "${REPO_FULLPATH}"/dljc-out/*.log
            echo "Start of toplevel.log:"
            cat "${REPO_FULLPATH}"/dljc-out/toplevel.log
            echo "End of toplevel.log."
            echo "Start of wpi-stdout.log:"
            cat "${REPO_FULLPATH}"/dljc-out/wpi-stdout.log
            echo "End of wpi-stdout.log."
            wpi_status=9999
        fi
        if [ "$DEBUG" -eq "1" ]; then
            echo "RESULT_LOG=${RESULT_LOG}"
            echo "TYPECHECK_FILE=${TYPECHECK_FILE}"
            ls -l "${TYPECHECK_FILE}"
            ls -l "${OUTDIR}-results/${REPO_NAME_HASH}-typecheck.out"
            echo "Listing of ${OUTDIR}-results:"
            ls -al "${OUTDIR}-results"
        fi
        if [ ! -s "${RESULT_LOG}" ] ; then
            echo "File does not exist: ${RESULT_LOG}"
            wpi_status=9999
        fi
        if [ ! -e "${OUTDIR}-results/${REPO_NAME_HASH}-typecheck.out" ] ; then
            echo "File does not exist: ${OUTDIR}-results/${REPO_NAME_HASH}-typecheck.out"
            wpi_status=9999
        fi
        if [[ "$wpi_status" != 0 ]]; then
            echo "Listing of ${OUTDIR}-results:"
            ls -al "${OUTDIR}-results"
            echo "==== start of ${OUTDIR}-results/wpi-out; printed because wpi_status=${wpi_status} ===="
            cat "${OUTDIR}-results/wpi-out"
            echo "==== end of ${OUTDIR}-results/wpi-out ===="
            exit 5
        fi
    fi
    # Avoid interleaved output from different iterations of the loop.
    sleep 1

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
echo "results_available = ${results_available}"

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
    # Don't match arguments like "-J--add-opens=jdk.compiler/com.sun.tools.java"
    # or "--add-opens=jdk.compiler/com.sun.tools.java".
    # shellcheck disable=SC2046
    grep -oh "^\S*\.java" $(cat "${OUTDIR}-results/results_available.txt") | sed "s/'//g" | grep -v '^\-J' | grep -v '^\-\-add\-opens' | sort | uniq > "${listpath}"

    if [ ! -s "${listpath}" ] ; then
        echo "listpath ${listpath} has size zero"
        ls -al "${listpath}"
        echo "results_available = ${results_available}"
        echo "---------------- start of ${OUTDIR}-results/results_available.txt ----------------"
        cat "${OUTDIR}-results/results_available.txt"
        echo "---------------- end of ${OUTDIR}-results/results_available.txt ----------------"
        echo "---------------- start of names of log files from which results_available.txt was constructed ----------------"
        ls -al "${OUTDIR}-results/"*.log
        echo "---------------- end of names of log files from which results_available.txt was constructed ----------------"
        ## This is too much output; Azure cuts it off.
        # echo "---------------- start of log files from which results_available.txt was constructed ----------------"
        # cat "${OUTDIR}-results/"*.log
        # echo "---------------- end of log files from which results_available.txt was constructed ----------------"
        exit 1
    fi

    mkdir -p "${SCRIPTDIR}/.scc"
    cd "${SCRIPTDIR}/.scc" || exit 5
    wget -nc "https://github.com/boyter/scc/releases/download/v2.13.0/scc-2.13.0-i386-unknown-linux.zip" \
      || (sleep 60s && wget -nc "https://github.com/boyter/scc/releases/download/v2.13.0/scc-2.13.0-i386-unknown-linux.zip")
    unzip -o "scc-2.13.0-i386-unknown-linux.zip"

    # shellcheck disable=SC2046
    if ! "${SCRIPTDIR}/.scc/scc" --output "${OUTDIR}-results/loc.txt" $(< "${listpath}") ; then
      echo "Problem in wpi-many.sh while running scc."
      echo "  listpath = ${listpath}"
      echo "  generated from ${OUTDIR}-results/results_available.txt"
      echo "---------------- start of listpath = ${listpath} ----------------"
      cat "${listpath}"
      echo "---------------- end of ${listpath} ----------------"
      echo "---------------- start of ${OUTDIR}-results/results_available.txt ----------------"
      cat "${OUTDIR}-results/results_available.txt"
      echo "---------------- end of ${OUTDIR}-results/results_available.txt ----------------"
      echo "---------------- start of names of log files from which results_available.txt was constructed ----------------"
      ls -al "${OUTDIR}-results/"*.log
      echo "---------------- end of names of log files from which results_available.txt was constructed ----------------"
      ## This is too much output; Azure cuts it off.
      # echo "---------------- start of log files from which results_available.txt was constructed ----------------"
      # cat "${OUTDIR}-results/"*.log
      # echo "---------------- end of log files from which results_available.txt was constructed ----------------"
      exit 1
    fi
    rm -f "${listpath}"
  else
    echo "skipping computation of lines of code because the operating system is not linux: ${OSTYPE}}"
  fi
fi

echo "Exiting wpi-many.sh successfully. Results were placed in ${OUTDIR}-results/."
