# This Makefile checks and enforces style.
# Most build system functionality exists in file `build.gradle`.

all default: style-check

# Code style; defines `style-check` and `style-fix`.
ifeq (,$(wildcard .plume-scripts))
dummy := $(shell git clone -q https://github.com/plume-lib/plume-scripts.git .plume-scripts)
endif
include .plume-scripts/code-style.mak
