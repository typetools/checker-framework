#!/bin/sh

# This script performs whole-program inference on a project.
# Run it from the project root.
# Its output is a directory `whole-program-inference-output/`
# that contains .ajava files.

# For usage, see the "Whole-program inference"
# section of the Checker Framework manual:
# https://checkerframework.org/manual/#whole-program-inference

# Exit the script if any statement fails.
set -e

rm -rf build/whole-program-inference
mkdir -p build/whole-program-inference
rm -rf whole-program-inference-output
mkdir -p whole-program-inference-output
touch whole-program-inference-output/first-iteration.txt

# The `wpi.diff` file is not used; it is produced for diagnostic purposes.
while ! diff -ur whole-program-inference-output build/whole-program-inference > wpi.diff; do
  # The command did not write any inference output; for example, the build system considered its
  # compilation tasks up to date and did not re-run them.  Stop rather than proceeding, because
  # the rest of the loop body would delete the output of the previous iterations.
  if [ ! -d build/whole-program-inference ]; then
    echo "wpi2.sh: $* did not create build/whole-program-inference." 1>&2
    echo "wpi2.sh: The output of the previous iterations is in whole-program-inference-output/." 1>&2
    exit 1
  fi
  rm -rf whole-program-inference-output
  mv build/whole-program-inference/ whole-program-inference-output
  mv -f wpi.diff wpi-prev.diff
  "$@"
done

rm -rf wpi.diff wpi-prev.diff
