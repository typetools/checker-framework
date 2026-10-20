#!/bin/bash

# Clones plume-scripts into checker/bin-devel/.plume-scripts, or updates an
# existing clone.  Every CI configuration runs this.
#
# See clone-or-update.sh for why this does not run the "getPlumeScripts" Gradle
# task.

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

exec "$SCRIPT_DIR"/clone-or-update.sh \
  https://github.com/plume-lib/plume-scripts.git "$SCRIPT_DIR"/.plume-scripts
