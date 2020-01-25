#!/bin/sh

# Clones the annotated JDK 11 into ../jdk .

if [ -d "/tmp/plume-scripts" ] ; then
  git -C /tmp/plume-scripts pull -q > /dev/null 2>&1
else
  git -C /tmp clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git
fi

git-clone-related typetools jdk
