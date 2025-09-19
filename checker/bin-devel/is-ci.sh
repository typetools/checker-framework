#!/bin/sh

# Prints "yes" if this process is running under CI.  Prints nothing otherwise.

if [ -n "$CI" ]; then echo "yes"
elif [ -n "$GITHUB_ACTION" ]; then echo "yes"
elif [ -n "$TRAVIS" ]; then echo "yes"
elif [ -n "$AZURE_HTTP_USER_AGENT" ]; then echo "yes"
elif [ -n "$CIRCLE_PR_USERNAME" ]; then echo "yes"
fi

