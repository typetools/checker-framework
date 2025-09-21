#!/bin/sh

# Prints "yes" if this process is running under CI.  Prints nothing otherwise.

if [ -n "$CI" ]; then
  echo "yes"
elif [ -n "$APPVEYOR" ]; then
  echo "yes"
elif [ -n "$AZURE_HTTP_USER_AGENT" ]; then
  echo "yes"
elif [ -n "$CIRCLECI" ]; then
  echo "yes"
elif [ -n "$GITHUB_ACTIONS" ]; then
  echo "yes"
elif [ -n "$GITLAB_CI" ]; then
  echo "yes"
elif [ -n "$TRAVIS" ]; then
  echo "yes"
else
  true
fi
