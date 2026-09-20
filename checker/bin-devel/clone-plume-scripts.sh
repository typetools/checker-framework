#!/bin/bash

# Clones plume-scripts into checker/bin-devel/.plume-scripts, retrying once in
# case the first attempt hit a transient network problem.
#
# Every CI configuration runs this, rather than "./gradlew getPlumeScripts",
# because "getPlumeScripts" is defined in "buildSrc".  Running it compiles
# "buildSrc", which resolves JGit, Bouncy Castle, and the Spotless plugin
# against Maven Central, and Maven Central sometimes rejects such a request
# with HTTP status code 403.  Cloning a repository of shell scripts should not
# depend on Maven Central at all.

echo "Entering checker/bin-devel/clone-plume-scripts.sh in $(pwd)"

# Fail the whole script if any command fails
set -e

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PLUME_SCRIPTS="${SCRIPT_DIR}/.plume-scripts"

clone_plume_scripts() {
  # A failed clone can leave a non-empty directory, which would make every
  # later attempt fail with "destination path already exists".
  rm -rf "$PLUME_SCRIPTS"
  git clone --depth=1 -q https://github.com/plume-lib/plume-scripts.git "$PLUME_SCRIPTS"
}

clone_plume_scripts || (sleep 60 && clone_plume_scripts)

echo "Exiting checker/bin-devel/clone-plume-scripts.sh in $(pwd)"
