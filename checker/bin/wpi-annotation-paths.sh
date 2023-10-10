#!/bin/sh

# Given the name of a directory containing the results produced by an
# invocation of wpi-many.sh and a filename, output the paths to the .ajava
# files produced by whole-program inference to a .txt file with the given name.

# Usage:
#   wpi-annotation-paths TARGETDIR RESULT_FILE_NAME

TARGETDIR="$1"
RESULT_FILE_NAME="$2"

if [ "$#" -ne 2 ]; then
  echo "Usage: $(basename "$0") TARGETDIR RESULT_FILE_NAME" >&2
  exit 1
fi

for wpi_log_file in $TARGETDIR/*-wpi-stdout.log;
do
  printf "Log file: $wpi_log_file\n" >> "$RESULT_FILE_NAME"

  FULL_AJAVA_MATCH=$(grep -oE 'Aajava=/[^ ]+' "$wpi_log_file" | tail -1)
  AJAVA_PATH=""
  if [ ! -z "$FULL_AJAVA_MATCH" ]; then
    AJAVA_PATH=$(echo "$FULL_AJAVA_MATCH" | cut -d "=" -f 2)
  else
    AJAVA_PATH="No .ajava files generated"
  fi

  printf "Annotated file(s): $AJAVA_PATH\n" >> "$RESULT_FILE_NAME"
done
