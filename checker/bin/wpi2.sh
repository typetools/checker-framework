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

if [ $# -eq 0 ]; then
  echo "Usage: wpi2.sh COMMAND [ARG...]" 1>&2
  echo "  COMMAND builds the project, running the Checker Framework with -Ainfer." 1>&2
  exit 2
fi

# The directory that the compiler writes inference results to; that is, the
# value of the -AinferOutputDirectory command-line argument.
newdir=whole-program-inference-new
# The directory that the compiler reads inference results from; that is, the
# value of the -Aajava command-line argument.  It is also this script's output.
outdir=whole-program-inference-output
# The directory that holds, for diagnostic purposes, the annotations that each
# iteration added.
diffdir=whole-program-inference-diffs

# The maximum number of times to run the command.  Inference usually converges
# after a few iterations, so more iterations than this suggests that it never
# will.  Set the WPI2_MAX_ITERATIONS environment variable to change the bound.
max_iterations=${WPI2_MAX_ITERATIONS:-10}

if [ -d "$outdir" ] && [ -n "$(ls -A "$outdir")" ]; then
  echo "wpi2.sh: continuing inference from the annotations in $outdir/." 1>&2
  echo "wpi2.sh: To start over, remove $outdir/ before running wpi2.sh." 1>&2
else
  mkdir -p "$outdir"
fi

rm -rf "$diffdir"
mkdir -p "$diffdir"

iteration=0
while : ; do
  iteration=$((iteration + 1))
  if [ "$iteration" -gt "$max_iterations" ]; then
    echo "wpi2.sh: inference did not converge after $max_iterations iterations." 1>&2
    echo "wpi2.sh: The output of the last iteration is in $outdir/," 1>&2
    echo "wpi2.sh: and $diffdir/ shows what each iteration changed." 1>&2
    echo "wpi2.sh: Set WPI2_MAX_ITERATIONS to run more iterations." 1>&2
    exit 1
  fi

  rm -rf "$newdir"
  "$@"

  # The command did not write any inference output; for example, the build system considered its
  # compilation tasks up to date and did not re-run them.  Stop rather than proceeding, because
  # the rest of the loop body would delete the output of the previous iterations.  This test also
  # rejects a directory that the compiler created but wrote no files to, which happens when the
  # build system recompiled only some of the project's source files.
  if [ ! -d "$newdir" ] || [ -z "$(ls -A "$newdir")" ]; then
    echo "wpi2.sh: $* did not write any files to $newdir/." 1>&2
    echo "wpi2.sh: The command must compile every source file of the project," 1>&2
    echo "wpi2.sh: passing -Ainfer=ajava and -AinferOutputDirectory=<absolute path to $newdir>." 1>&2
    if [ "$iteration" -gt 1 ]; then
      echo "wpi2.sh: The output of the previous iterations is in $outdir/." 1>&2
    fi
    exit 1
  fi

  diffstatus=0
  diff -ur "$outdir" "$newdir" > "$diffdir/iteration-$iteration.diff" || diffstatus=$?
  # `diff` exits with status 1 if the directories differ, and with status 2 or more if it failed.
  if [ "$diffstatus" -gt 1 ]; then
    echo "wpi2.sh: \`diff -ur $outdir $newdir\` failed with status $diffstatus." 1>&2
    echo "wpi2.sh: The output of the previous iterations is in $outdir/." 1>&2
    exit 1
  fi

  if [ "$diffstatus" -eq 0 ]; then
    rm -rf "$newdir"
    if [ "$iteration" -eq 1 ]; then plural=""; else plural="s"; fi
    echo "wpi2.sh: inference converged after $iteration iteration$plural; its output is in $outdir/."
    exit 0
  fi

  rm -rf "$outdir"
  mv "$newdir" "$outdir"
done
