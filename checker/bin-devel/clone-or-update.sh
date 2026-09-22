#!/bin/bash

# Clones a git repository, or updates an existing clone.  Retries cloning once,
# after a delay, in case the failure was a transient network problem.
#
# Usage: clone-or-update.sh URL DIRECTORY
#
# Use this rather than one of the "get*" Gradle tasks -- getPlumeScripts,
# getGitScripts, getPlumeBib, getDoLikeJavac -- when nothing else in the same
# command needs Gradle.  Those tasks are defined in "buildSrc", so running one
# compiles "buildSrc", which resolves JGit, Bouncy Castle, and the Spotless
# plugin against Maven Central.  Maven Central sometimes rejects such a request
# with HTTP status code 403, which would fail the whole command.  Cloning a
# repository should not depend on Maven Central at all.
#
# Within a Gradle build, depend on the task instead; "buildSrc" is compiled in
# that case regardless, so the task costs nothing extra.

# echo "Entering checker/bin-devel/clone-or-update.sh $* in $(pwd)"

# Fail the whole script if any command fails
set -e

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 URL DIRECTORY" >&2
  exit 2
fi

URL="$1"
DIRECTORY="$2"

clone_or_update() {
  if [ -d "$DIRECTORY/.git" ]; then
    if git -C "$DIRECTORY" pull -q; then
      return 0
    fi
    # Keep the clone that is already there.  It is usable even when out of
    # date, and the failure is more likely a network problem than a corrupt
    # clone.  This is what the "CloneOrUpdateTask" Gradle task does too.
    echo "clone-or-update.sh: cannot update $DIRECTORY; using it as it is." >&2
    return 0
  fi
  # An interrupted clone can leave a non-empty directory, which would make
  # "git clone" fail with "destination path already exists".
  rm -rf "$DIRECTORY"
  git clone --depth=1 -q "$URL" "$DIRECTORY"
}

clone_or_update || (sleep 60 && clone_or_update)

# echo "Exiting checker/bin-devel/clone-or-update.sh $* in $(pwd)"
