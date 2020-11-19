#!/bin/bash

# This script performs whole-program inference on a project directory.

# For usage and requirements, see the "Whole-program inference"
# section of the Checker Framework manual:
# https://checkerframework.org/manual/#whole-program-inference


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

# check required arguments and environment variables:

# Testing for JAVA8_HOME, not a misspelling of JAVA_HOME.
# shellcheck disable=SC2153
if [ "x${JAVA8_HOME}" = "x" ]; then
    echo "JAVA8_HOME must be set to a Java 8 JDK"
    exit 1
fi

if [ ! -d "${JAVA8_HOME}" ]; then
    echo "JAVA8_HOME is set to a non-existent directory ${JAVA8_HOME}"
    exit 1
fi

# Testing for JAVA11_HOME, not a misspelling of JAVA_HOME.
# shellcheck disable=SC2153
if [ "x${JAVA11_HOME}" = "x" ]; then
    echo "JAVA11_HOME must be set to a Java 11 JDK"
    exit 1
fi

if [ ! -d "${JAVA11_HOME}" ]; then
    echo "JAVA11_HOME is set to a non-existent directory ${JAVA11_HOME}"
    exit 1
fi

JAVA_HOME="${JAVA11_HOME}"

if [ "x${CHECKERFRAMEWORK}" = "x" ]; then
    echo "CHECKERFRAMEWORK is not set; it must be set to a locally-built Checker Framework. Please clone and build github.com/typetools/checker-framework"
    exit 2
fi

if [ ! -d "${CHECKERFRAMEWORK}" ]; then
    echo "CHECKERFRAMEWORK is set to a non-existent directory ${CHECKERFRAMEWORK}"
    exit 2
fi

if [ "x${DIR}" = "x" ]; then
    echo "wpi.sh: no -d argument supplied, using the current directory."
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
      # if running on java 8, need /jre at the end of this Maven command
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
      WPI_RESULTS_AVAILABLE="no"
      return
  fi

  DLJC_CMD="${DLJC} -t wpi $* -- ${BUILD_CMD}"

  if [ ! "x${TIMEOUT}" = "x" ]; then
      TMP="${DLJC_CMD}"
      DLJC_CMD="timeout ${TIMEOUT} ${TMP}"
  fi

  # Remove old DLJC output.
  rm -rf dljc-out

  # ensure the project is clean before invoking DLJC
  eval "${CLEAN_CMD}" < /dev/null > /dev/null 2>&1

  tmpfile=$(mktemp "${DIR}/dljc-output/dljc-stdout.XXXXXX")

  # This command also includes "clean"; I'm not sure why it is necessary.
  { echo "JAVA_HOME: ${JAVA_HOME}"; \
    echo "DLJC_CMD: ${DLJC_CMD}"; \
    eval "${DLJC_CMD}" < /dev/null; } > "$tmpfile" 2>&1

  if [[ $? -eq 124 ]]; then
      echo "dljc timed out for ${DIR}"
      WPI_RESULTS_AVAILABLE="no"
      return
  fi

  if [ -f dljc-out/wpi.log ]; then
      echo "dljc output is available in ${DIR}/dljc-out/; stdout is in $tmpfile"
      WPI_RESULTS_AVAILABLE="yes"
  else
      echo "dljc output is not available in ${DIR}/dljc-out/; stdout is in $tmpfile"
      WPI_RESULTS_AVAILABLE="no"
  fi
}

#### Check and setup dependencies

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

# clone or update DLJC
if [ -d "${SCRIPTDIR}/.do-like-javac" ]; then
    git -C "${SCRIPTDIR}/.do-like-javac" pull
else
    git -C "${SCRIPTDIR}" clone https://github.com/kelloggm/do-like-javac --depth 1 .do-like-javac || (echo "Cannot clone do-like-javac" && exit 1)
fi

DLJC="${SCRIPTDIR}/.do-like-javac/dljc"

#### Main script

echo "Starting wpi.sh. The output of this script is purely informational. Results will be placed in ${DIR}/dljc-out/."

rm -f rm -f "${DIR}/.cannot-run-wpi"

(cd "${DIR}" && configure_and_exec_dljc "$@")

if [ "${WPI_RESULTS_AVAILABLE}" = "no" ]; then
      # if running under Java 11 fails, try to run
      # under Java 8 instead
    export JAVA_HOME="${JAVA8_HOME}"
    echo "couldn't build using Java 11; trying Java 8"
    configure_and_exec_dljc "$@"
    export JAVA_HOME="${JAVA11_HOME}"
fi

# support wpi-many.sh's ability to delete projects without usable results
# automatically
if [ "${WPI_RESULTS_AVAILABLE}" = "no" ]; then
    echo "dljc could not run the build successfully."
    echo "Check the log files in ${DIR}/dljc-out/ for diagnostics."
    touch "${DIR}/.cannot-run-wpi"
fi

echo "Exiting wpi.sh. The output of this script was purely informational. Results were placed in ${DIR}/dljc-out/."
