#!/bin/bash

# This script performs whole-program inference on a project directory.

# For usage and requirements, see the "Whole-program inference"
# section of the Checker Framework manual:
# https://checkerframework.org/manual/#whole-program-inference

set -eo pipefail
# not set -u, because this script checks variables directly

while getopts "d:t:b:g:" opt; do
  case $opt in
    d) DIR="$OPTARG"
       ;;
    t) TIMEOUT="$OPTARG"
       ;;
    b) EXTRA_BUILD_ARGS="$OPTARG"
       ;;
    g) GRADLECACHEDIR="$OPTARG"
       ;;
    \?) # echo "Invalid option -$OPTARG" >&2
       ;;
  esac
done

# Make $@ be the arguments that should be passed to dljc.
shift $(( OPTIND - 1 ))

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
SCRIPTPATH="${SCRIPTDIR}/wpi.sh"

# Report line numbers when the script fails, from
# https://unix.stackexchange.com/a/522815
trap 'echo >&2 "Error - exited with status $? at line $LINENO of wpi.sh:";
         pr -tn $SCRIPTPATH | tail -n+$((LINENO - 3)) | head -n7' ERR

echo "Starting wpi.sh."

# check required arguments and environment variables:

if [ "x${JAVA_HOME}" = "x" ]; then
  has_java_home="no"
else
  has_java_home="yes"
fi

# shellcheck disable=SC2153 # testing for JAVA8_HOME, not a typo of JAVA_HOME
if [ "x${JAVA8_HOME}" = "x" ]; then
  has_java8="no"
else
  has_java8="yes"
fi

# shellcheck disable=SC2153 # testing for JAVA11_HOME, not a typo of JAVA_HOME
if [ "x${JAVA11_HOME}" = "x" ]; then
  has_java11="no"
else
  has_java11="yes"
fi

# shellcheck disable=SC2153 # testing for JAVA16_HOME, not a typo of JAVA_HOME
if [ "x${JAVA16_HOME}" = "x" ]; then
  has_java16="no"
else
  has_java16="yes"
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
    if [ "${has_java16}" = "no" ] && [ "${java_version}" = 16 ]; then
      export JAVA16_HOME="${JAVA_HOME}"
      has_java16="yes"
    fi
fi

if [ "${has_java8}" = "yes" ] && [ ! -d "${JAVA8_HOME}" ]; then
    echo "JAVA8_HOME is set to a non-existent directory ${JAVA8_HOME}"
    exit 6
fi

if [ "${has_java11}" = "yes" ] && [ ! -d "${JAVA11_HOME}" ]; then
    echo "JAVA11_HOME is set to a non-existent directory ${JAVA11_HOME}"
    exit 7
fi

if [ "${has_java16}" = "yes" ] && [ ! -d "${JAVA16_HOME}" ]; then
    echo "JAVA16_HOME is set to a non-existent directory ${JAVA16_HOME}"
    exit 7
fi

if [ "${has_java8}" = "no" ] && [ "${has_java11}" = "no" ] && [ "${has_java16}" = "no" ]; then
    echo "No Java 8, 11, or 16 JDKs found. At least one of JAVA_HOME, JAVA8_HOME, JAVA11_HOME, or JAVA16_HOME must be set."
    exit 8
fi

if [ "x${CHECKERFRAMEWORK}" = "x" ]; then
    echo "CHECKERFRAMEWORK is not set; it must be set to a locally-built Checker Framework. Please clone and build github.com/typetools/checker-framework"
    exit 2
fi

if [ ! -d "${CHECKERFRAMEWORK}" ]; then
    echo "CHECKERFRAMEWORK is set to a non-existent directory ${CHECKERFRAMEWORK}"
    exit 9
fi

if [ "x${DIR}" = "x" ]; then
    # echo "wpi.sh: no -d argument supplied, using the current directory."
    DIR=$(pwd)
fi

if [ ! -d "${DIR}" ]; then
    echo "wpi.sh's -d argument was not a directory: ${DIR}"
    exit 4
fi

if [ "x${EXTRA_BUILD_ARGS}" = "x" ]; then
  EXTRA_BUILD_ARGS=""
fi

if [ "x${GRADLECACHEDIR}" = "x" ]; then
  # Assume that each project should use its own gradle cache. This is more expensive,
  # but prevents crashes on distributed file systems, such as the UW CSE machines.
  GRADLECACHEDIR=".gradle"
fi

function configure_and_exec_dljc {

  if [ -f build.gradle ]; then
      if [ -f gradlew ]; then
        chmod +x gradlew
        GRADLE_EXEC="./gradlew"
      else
        GRADLE_EXEC="gradle"
      fi
      if [ ! -d "${GRADLECACHEDIR}" ]; then
        mkdir "${GRADLECACHEDIR}"
      fi
      CLEAN_CMD="${GRADLE_EXEC} clean -g ${GRADLECACHEDIR} -Dorg.gradle.java.home=${JAVA_HOME} ${EXTRA_BUILD_ARGS}"
      BUILD_CMD="${GRADLE_EXEC} clean compileJava -g ${GRADLECACHEDIR} -Dorg.gradle.java.home=${JAVA_HOME} ${EXTRA_BUILD_ARGS}"
  elif [ -f pom.xml ]; then
      if [ -f mvnw ]; then
        chmod +x mvnw
        MVN_EXEC="./mvnw"
      else
        MVN_EXEC="mvn"
      fi
      # if running on Java 8, need /jre at the end of this Maven command
      if [ "${JAVA_HOME}" = "${JAVA8_HOME}" ]; then
          CLEAN_CMD="${MVN_EXEC} clean -Djava.home=${JAVA_HOME}/jre ${EXTRA_BUILD_ARGS}"
          BUILD_CMD="${MVN_EXEC} clean compile -Djava.home=${JAVA_HOME}/jre ${EXTRA_BUILD_ARGS}"
      else
          CLEAN_CMD="${MVN_EXEC} clean -Djava.home=${JAVA_HOME} ${EXTRA_BUILD_ARGS}"
          BUILD_CMD="${MVN_EXEC} clean compile -Djava.home=${JAVA_HOME} ${EXTRA_BUILD_ARGS}"
      fi
  elif [ -f build.xml ]; then
    # TODO: test these more thoroughly
    CLEAN_CMD="ant clean ${EXTRA_BUILD_ARGS}"
    BUILD_CMD="ant clean compile ${EXTRA_BUILD_ARGS}"
  else
      echo "no build file found for ${REPO_NAME}; not calling DLJC"
      WPI_RESULTS_AVAILABLE="no build file found for ${REPO_NAME}"
      return
  fi

  if [ "${JAVA_HOME}" = "${JAVA8_HOME}" ]; then
    JDK_VERSION_ARG="--jdkVersion 8"
  elif [ "${JAVA_HOME}" = "${JAVA11_HOME}" ]; then
    JDK_VERSION_ARG="--jdkVersion 11"
  elif [ "${JAVA_HOME}" = "${JAVA16_HOME}" ]; then
    JDK_VERSION_ARG="--jdkVersion 16"
  else
    # Default to the latest LTS release.  (Probably better to compute the version.)
    JDK_VERSION_ARG="--jdkVersion 11"
  fi

  # In bash 4.4, ${QUOTED_ARGS} below can be replaced by ${*@Q} .
  # (But, this script does not assume that bash is at least version 4.4.)
  QUOTED_ARGS=$(printf '%q ' "$@")

  # This command also includes "clean"; I'm not sure why it is necessary.
  DLJC_CMD="${DLJC} -t wpi ${JDK_VERSION_ARG} ${QUOTED_ARGS} -- ${BUILD_CMD}"

  if [ ! "x${TIMEOUT}" = "x" ]; then
      TMP="${DLJC_CMD}"
      DLJC_CMD="timeout ${TIMEOUT} ${TMP}"
  fi

  # Remove old DLJC output.
  rm -rf dljc-out

  # Ensure the project is clean before invoking DLJC.
  # If it fails, re-run without piping output to /dev/null.
  eval "${CLEAN_CMD}" < /dev/null > /dev/null 2>&1 || eval "${CLEAN_CMD}" < /dev/null

  mkdir -p "${DIR}/dljc-out/"
  dljc_stdout=$(mktemp "${DIR}/dljc-out/dljc-stdout-$(date +%Y%m%d-%H%M%S)-XXX")

  PATH_BACKUP="${PATH}"
  export PATH="${JAVA_HOME}/bin:${PATH}"

  # shellcheck disable=SC2129 # recommended syntax was crashing mysteriously in CI
  echo "WORKING DIR: $(pwd)" >> "$dljc_stdout"
  echo "JAVA_HOME: ${JAVA_HOME}" >> "$dljc_stdout"
  echo "PATH: ${PATH}" >> "$dljc_stdout"
  echo "DLJC_CMD: ${DLJC_CMD}" >> "$dljc_stdout"
  DLJC_STATUS=0
  eval "${DLJC_CMD}" < /dev/null >> "$dljc_stdout" 2>&1 || DLJC_STATUS=$?

  export PATH="${PATH_BACKUP}"

  echo "=== DLJC standard out/err (${dljc_stdout}) follows: ==="
  cat "${dljc_stdout}"
  echo "=== End of DLJC standard out/err.  ==="

  # the wpi.py script in do-like-javac outputs the following text if no build/whole-program-inference directory
  # exists, which means that WPI produced no output. When that happens, the reason is usually that the Checker
  # Framework crashed, so output the log file for easier debugging.
  wpi_no_output_message="No WPI outputs were discovered; it is likely that WPI failed or the Checker Framework crashed"
  if [[ $(cat "${dljc_stdout}") == *"${wpi_no_output_message}"* ]]; then
    wpi_log_path="${DIR}"/dljc-out/wpi.log
    echo "=== ${wpi_no_output_message}: printing ${wpi_log_path} ==="
    cat "${wpi_log_path}"
    echo "=== end of ${wpi_log_path} ==="
  fi

  if [[ $DLJC_STATUS -eq 124 ]]; then
      echo "dljc timed out for ${DIR}"
      WPI_RESULTS_AVAILABLE="dljc timed out for ${DIR}"
      return
  fi

  if [ -f dljc-out/wpi.log ]; then
      # Put, in file `typecheck.out`, everything from the last "Running ..." onwards.
      sed -n '/^Running/h;//!H;$!d;x;//p' dljc-out/wpi.log > dljc-out/typecheck.out
      WPI_RESULTS_AVAILABLE="yes"
      echo "dljc output is in ${DIR}/dljc-out/"
      echo "typecheck output is in ${DIR}/dljc-out/typecheck.out"
      echo "stdout is in $dljc_stdout"
  else
      WPI_RESULTS_AVAILABLE="file ${DIR}/dljc-out/wpi.log does not exist"
      echo "dljc failed: ${WPI_RESULTS_AVAILABLE}"
      echo "dljc output is in ${DIR}/dljc-out/"
      echo "stdout is in $dljc_stdout"
  fi
}

#### Check and setup dependencies

# Clone or update DLJC
if [ "${DLJC}x" = "x" ]; then
  # The user did not set the DLJC environment variable.
  (cd "${SCRIPTDIR}"/../.. && ./gradlew getPlumeScripts -q)
  "${SCRIPTDIR}"/../bin-devel/.plume-scripts/git-clone-related kelloggm do-like-javac "${SCRIPTDIR}"/.do-like-javac
  if [ ! -d "${SCRIPTDIR}/.do-like-javac" ]; then
      echo "Failed to clone do-like-javac"
      exit 1
  fi
  DLJC="${SCRIPTDIR}/.do-like-javac/dljc"
else
  # The user did set the DLJC environment variable.
  if [ ! -f "${DLJC}" ]; then
    echo "Failure: DLJC is set to ${DLJC} which is not a file or does not exist."
    exit 1
  fi
fi

#### Main script

echo "Finished configuring wpi.sh."

rm -f -- "${DIR}/.cannot-run-wpi"

cd "${DIR}" || exit 5

JAVA_HOME_BACKUP="${JAVA_HOME}"
if [ "${has_java8}" = "yes" ]; then
  export JAVA_HOME="${JAVA8_HOME}"
elif [ "${has_java11}" = "yes" ]; then
  export JAVA_HOME="${JAVA11_HOME}"
elif [ "${has_java16}" = "yes" ]; then
  export JAVA_HOME="${JAVA16_HOME}"
fi
configure_and_exec_dljc "$@"

if [ "${has_java11}" = "yes" ] && [ "${WPI_RESULTS_AVAILABLE}" != "yes" ]; then
    # If running under Java 11 fails, try Java 8.
    if [ "${has_java8}" = "yes" ]; then
      export JAVA_HOME="${JAVA8_HOME}"
      echo "couldn't build using Java 11; trying Java 8"
      configure_and_exec_dljc "$@"
    fi
fi

if [ "${has_java11}" = "yes" ] && [ "${has_java8}" = "yes" ] && [ "${WPI_RESULTS_AVAILABLE}" != "yes" ]; then
    # If running under Java 11 and Java 8 fails, try Java 16.
    if [ "${has_java16}" = "yes" ]; then
      export JAVA_HOME="${JAVA16_HOME}"
      echo "couldn't build using Java 11 or Java 8; trying Java 16"
      configure_and_exec_dljc "$@"
    fi
fi

# support wpi-many.sh's ability to delete projects without usable results
# automatically
if [ "${WPI_RESULTS_AVAILABLE}" != "yes" ]; then
    echo "dljc could not run the build successfully: ${WPI_RESULTS_AVAILABLE}"
    echo "Check the log files in ${DIR}/dljc-out/ for diagnostics."
    echo "${WPI_RESULTS_AVAILABLE}" > "${DIR}/.cannot-run-wpi"
fi

export JAVA_HOME="${JAVA_HOME_BACKUP}"

echo "Exiting wpi.sh."
