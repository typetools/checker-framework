#!/bin/bash

set -e
set -o verbose
set -o xtrace
export SHELLOPTS
echo "SHELLOPTS=${SHELLOPTS}"

# Optional argument $1 is the group.
GROUPARG=$1
echo "GROUPARG=$GROUPARG"
# These are all the Java projects at https://github.com/eisop-plume-lib
if [[ "${GROUPARG}" == "bcel-util" ]]; then PACKAGES=("${GROUPARG}"); fi
if [[ "${GROUPARG}" == "bibtex-clean" ]]; then PACKAGES=("${GROUPARG}"); fi
if [[ "${GROUPARG}" == "html-pretty-print" ]]; then PACKAGES=("${GROUPARG}"); fi
if [[ "${GROUPARG}" == "icalavailable" ]]; then PACKAGES=("${GROUPARG}"); fi
if [[ "${GROUPARG}" == "lookup" ]]; then PACKAGES=("${GROUPARG}"); fi
if [[ "${GROUPARG}" == "multi-version-control" ]]; then PACKAGES=("${GROUPARG}"); fi
if [[ "${GROUPARG}" == "options" ]]; then PACKAGES=("${GROUPARG}"); fi
if [[ "${GROUPARG}" == "plume-util" ]]; then PACKAGES=("${GROUPARG}"); fi
if [[ "${GROUPARG}" == "require-javadoc" ]]; then PACKAGES=("${GROUPARG}"); fi
if [[ "${GROUPARG}" == "all" ]] || [[ "${GROUPARG}" == "" ]]; then
    if java -version 2>&1 | grep version | grep 1.8 ; then
        # options does not compile under JDK 8
        PACKAGES=(bcel-util bibtex-clean html-pretty-print icalavailable lookup multi-version-control plume-util require-javadoc)
    elif java -version 2>&1 | grep version | grep 17 ; then
        # TODO bcel-util does not compile under JDK 17
        PACKAGES=(bibtex-clean html-pretty-print icalavailable lookup multi-version-control options plume-util require-javadoc)
    else
        PACKAGES=(bcel-util bibtex-clean html-pretty-print icalavailable lookup multi-version-control options plume-util require-javadoc)
    fi
fi
if [ -z ${PACKAGES+x} ]; then
  echo "Bad group argument '${GROUPARG}'"
  exit 1
fi
echo "PACKAGES=" "${PACKAGES[@]}"


SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
# shellcheck disable=SC1090 # In newer shellcheck than 0.6.0, pass: "-P SCRIPTDIR" (literally)
source "$SCRIPTDIR"/build.sh


echo "PACKAGES=" "${PACKAGES[@]}"
for PACKAGE in "${PACKAGES[@]}"; do
  echo "PACKAGE=${PACKAGE}"
  PACKAGEDIR="/tmp/${PACKAGE}"
  rm -rf "${PACKAGEDIR}"
  "$SCRIPTDIR/.plume-scripts/git-clone-related" eisop-plume-lib "${PACKAGE}" "${PACKAGEDIR}"
  # Uses "compileJava" target instead of "assemble" to avoid the javadoc error "Error fetching URL:
  # https://docs.oracle.com/en/java/javase/11/docs/api/" due to network problems.
  echo "About to call ./gradlew --console=plain -PcfLocal compileJava"
  # Try twice in case of network lossage while downloading packages (e.g., from Maven Central).
  # A disadvantage is that if there is a real error in pluggable type-checking, this runs it twice
  # and puts a delay in between.
  (cd "${PACKAGEDIR}" && (./gradlew --console=plain -PcfLocal compileJava || (sleep 60 && ./gradlew --console=plain -PcfLocal compileJava)))
done
