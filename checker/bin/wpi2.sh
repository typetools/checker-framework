#!/bin/sh

# This script performs whole-program inference on a project.  Run it from the
# project root.  Its output is a directory `whole-program-inference-output/`
# that contains .ajava files.

# For usage, see the "Whole-program inference" section of the Checker Framework
# manual:  https://checkerframework.org/manual/#whole-program-inference

# Exit the script if any statement fails.
set -e

# The directory that the compiler writes inference results to; that is, the
# value of the -AinferOutputDirectory command-line argument.
newdir=whole-program-inference-new
# The directory that the compiler reads inference results from; that is, the
# value of the -Aajava command-line argument.  It is also this script's output.
outdir=whole-program-inference-output
# The directory that holds, for diagnostic purposes, the annotations that each
# iteration added.
diffdir=whole-program-inference-diffs
# The directory that $outdir is moved to while $newdir replaces it.  It exists
# only during that replacement, or after a run that was interrupted during it.
prevdir=whole-program-inference-previous

if [ $# -eq 0 ]; then
  echo "Usage: wpi2.sh COMMAND [ARG...]" 1>&2
  echo "COMMAND builds the project.  It must supply the Checker Framework with" 1>&2
  echo "  -Ainfer=ajava" 1>&2
  echo "  -AinferOutputDirectory=$PWD/$newdir" 1>&2
  echo "  -Aajava=$PWD/$outdir" 1>&2
  echo "  -Awarns" 1>&2
  exit 2
fi

# The maximum number of times to run the command.  Inference usually converges
# after a few iterations, so more iterations than this suggests that it never
# will.  Set the WPI2_MAX_ITERATIONS environment variable to change the bound.
max_iterations=${WPI2_MAX_ITERATIONS:-10}
# If $max_iterations is not a number, or is too large for the shell, then the comparison fails
# rather than yielding false.  `set -e` does not halt a script when the failing command is an
# `if` condition, so suppress the comparison's error message and treat failure as invalid; a
# comparison that fails every time would make the loop below run forever.
if ! [ "$max_iterations" -ge 2 ] 2> /dev/null; then
  echo "wpi2.sh: WPI2_MAX_ITERATIONS must be an integer between 2 and the shell's maximum," 1>&2
  echo "wpi2.sh: but it is \"$WPI2_MAX_ITERATIONS\"." 1>&2
  exit 2
fi

# A directory for this script's own temporary files.
tmpdir=$(mktemp -d)
trap 'rm -rf "$tmpdir"' EXIT

# A previous run of wpi2.sh was interrupted while $newdir replaced $outdir.
# Recover the annotations that the interrupted run had moved aside.
if [ ! -d "$outdir" ] && [ -d "$prevdir" ]; then
  echo "wpi2.sh: An interrupted run of wpi2.sh left no $outdir/;" 1>&2
  echo "wpi2.sh: recovering it from $prevdir/." 1>&2
  mv "$prevdir" "$outdir"
elif [ -d "$prevdir" ]; then
  # $outdir exists, so $prevdir holds annotations that $outdir supersedes; an interrupted run
  # left $prevdir behind after it had finished replacing $outdir.  Remove $prevdir now.  If it
  # remained, then a later run that the user started over by removing $outdir would treat it as
  # annotations to recover, above, and would not start over after all.
  rm -rf "$prevdir"
fi

if [ -d "$outdir" ] && [ -n "$(find "$outdir" -type f | head -n 1)" ]; then
  echo "wpi2.sh: Continuing inference from the annotations in $outdir/." 1>&2
  echo "wpi2.sh: To start over, remove $outdir/ before running wpi2.sh." 1>&2
else
  mkdir -p "$outdir"
  # This run starts from scratch, so the previous run's diffs are irrelevant.
  rm -rf "$diffdir"
fi
mkdir -p "$diffdir"

# Number this run's diffs after those of any previous run, whose diffs this run
# retains because it continues where the previous run left off.
# Run find separately from the pipeline that parses its output, so that the
# exit status of the last pipeline stage does not mask a failure of find.
if ! find "$diffdir" -maxdepth 1 -name 'iteration-*.diff' > "$tmpdir/diff-files"; then
  echo "wpi2.sh: cannot list the diffs in $PWD/$diffdir/." 1>&2
  exit 1
fi
diffoffset=$(sed -n 's/.*iteration-\([0-9][0-9]*\)\.diff$/\1/p' "$tmpdir/diff-files" \
  | sort -n | tail -n 1)
if [ -z "$diffoffset" ]; then
  diffoffset=0
fi

iteration=0
while :; do
  iteration=$((iteration + 1))
  if [ "$iteration" -gt "$max_iterations" ]; then
    echo "wpi2.sh: Inference did not converge after $max_iterations iterations." 1>&2
    echo "wpi2.sh: The output of the last iteration is in $outdir/," 1>&2
    echo "wpi2.sh: and $diffdir/ shows what each iteration changed." 1>&2
    echo "wpi2.sh: Set WPI2_MAX_ITERATIONS to run more iterations." 1>&2
    exit 1
  fi

  rm -rf "$newdir"
  buildstatus=0
  "$@" || buildstatus=$?
  if [ "$buildstatus" -ne 0 ]; then
    echo "wpi2.sh: $* failed with status $buildstatus." 1>&2
    if [ -n "$(find "$outdir" -type f | head -n 1)" ]; then
      echo "wpi2.sh: The annotations inferred so far are in $outdir/;" 1>&2
      echo "wpi2.sh: re-running wpi2.sh continues from them." 1>&2
    fi
    exit "$buildstatus"
  fi

  if [ -d "$newdir" ]; then
    (cd "$newdir" && find . -type f) > "$tmpdir/newdir-files-unsorted"
    LC_ALL=C sort "$tmpdir/newdir-files-unsorted" > "$tmpdir/newdir-files"
  else
    : > "$tmpdir/newdir-files"
  fi

  # The command did not write any inference output; for example, the build system considered its
  # compilation tasks up to date and did not re-run them.  (The compiler might have created
  # directories in $newdir without writing any files to them, so this tests for files rather than
  # for directory entries.)  Stop rather than proceeding, because the rest of the loop body would
  # delete the output of the previous iterations.
  if [ ! -s "$tmpdir/newdir-files" ]; then
    echo "wpi2.sh: $* did not write any files to $newdir/." 1>&2
    if [ -d "$outdir" ] && [ -n "$(find "$outdir" -type f | head -n 1)" ]; then
      # A previous iteration inferred annotations, so the Checker Framework would have written
      # them again if the command had compiled the project.  (Every iteration writes an .ajava
      # file for every source file about which the Checker Framework infers anything.)
      echo "wpi2.sh: A previous iteration wrote files, so the command did not compile the" 1>&2
      echo "wpi2.sh: project this time.  The command must compile every source file of the" 1>&2
      echo "wpi2.sh: project, every time." 1>&2
      echo "wpi2.sh: The annotations inferred so far are in $outdir/;" 1>&2
      echo "wpi2.sh: re-running wpi2.sh continues from them." 1>&2
    else
      echo "wpi2.sh: Either the command did not compile the project, or the Checker Framework" 1>&2
      echo "wpi2.sh: inferred nothing about it.  The command must compile every source file" 1>&2
      echo "wpi2.sh: of the project, passing" 1>&2
      echo "  -Ainfer=ajava" 1>&2
      echo "  -AinferOutputDirectory=$PWD/$newdir" 1>&2
      echo "  -Aajava=$PWD/$outdir" 1>&2
      echo "  -Awarns" 1>&2
    fi
    exit 1
  fi

  # The command deleted $outdir, which holds the annotations that the previous iterations
  # inferred; for example, the build system's `clean` task removed it.  Stop rather than
  # proceeding, because the rest of the loop body requires $outdir to exist.
  if [ ! -d "$outdir" ]; then
    echo "wpi2.sh: $* deleted $outdir/, which held the annotations inferred so far." 1>&2
    echo "wpi2.sh: Do not put $outdir/ under your build system's output directory," 1>&2
    echo "wpi2.sh: whose \`clean\` task deletes it, possibly in the middle of a build." 1>&2
    mv "$newdir" "$outdir"
    echo "wpi2.sh: This iteration's annotations are in $outdir/;" 1>&2
    echo "wpi2.sh: re-running wpi2.sh continues from them." 1>&2
    exit 1
  fi

  # The command wrote some, but not all, of the inference output; for example, the build system
  # recompiled only some of the project's source files.  Stop rather than proceeding, because the
  # rest of the loop body would delete the annotations that were inferred for the files that the
  # command did not recompile.
  (cd "$outdir" && find . -type f) > "$tmpdir/outdir-files-unsorted"
  LC_ALL=C sort "$tmpdir/outdir-files-unsorted" > "$tmpdir/outdir-files"
  missing=$(LC_ALL=C comm -23 "$tmpdir/outdir-files" "$tmpdir/newdir-files")
  if [ -n "$missing" ]; then
    nmissing=$(echo "$missing" | wc -l | tr -d ' ')
    if [ "$nmissing" -eq 1 ]; then plural=""; else plural="s"; fi
    echo "wpi2.sh: $* did not write $nmissing file$plural to $newdir/ that a previous" 1>&2
    echo "wpi2.sh: iteration wrote to $outdir/:" 1>&2
    echo "$missing" | head -n 10 | sed 's|^\./|  |' 1>&2
    if [ "$nmissing" -gt 10 ]; then
      echo "  ... and $((nmissing - 10)) more" 1>&2
    fi
    echo "wpi2.sh: The command must compile every source file of the project, every time." 1>&2
    echo "wpi2.sh: The output of the previous iterations is in $outdir/.  If you deleted" 1>&2
    echo "wpi2.sh: source files since the previous iteration, remove $outdir/ and re-run." 1>&2
    exit 1
  fi

  diffstatus=0
  diffpath="$diffdir/iteration-$((diffoffset + iteration)).diff"
  diff -ur "$outdir" "$newdir" > "$diffpath" || diffstatus=$?
  # `diff` exits with status 1 if the directories differ, and with status 2 or more if it failed.
  if [ "$diffstatus" -gt 1 ]; then
    echo "wpi2.sh: \`diff -ur $outdir $newdir\` failed with status $diffstatus." 1>&2
    echo "wpi2.sh: The output of the previous iterations is in $outdir/." 1>&2
    exit 1
  fi

  if [ "$diffstatus" -eq 0 ]; then
    rm -rf "$newdir"
    # Count the iterations of previous runs too, so that this count is consistent with the
    # iteration numbers of the files in $diffdir.
    totaliterations=$((diffoffset + iteration))
    if [ "$totaliterations" -eq 1 ]; then plural=""; else plural="s"; fi
    echo "wpi2.sh: inference converged after $totaliterations iteration$plural; its output is in $outdir/."
    exit 0
  fi

  # Replace $outdir by $newdir.
  rm -rf "$prevdir"
  mv "$outdir" "$prevdir"
  mv "$newdir" "$outdir"
  rm -rf "$prevdir"
done
