# This Makefile checks and enforces style.
# Most build system functionality exists in file `build.gradle`.

all default: style-check

plume-scripts-dir = checker/bin-devel/.plume-scripts
# Code style; defines `style-check` and `style-fix`.
ifeq (,$(wildcard plume-scripts-dir))
dummy := $(shell git clone --depth=1 -q https://github.com/plume-lib/plume-scripts.git plume-scripts-dir)
endif
include plume-scripts-dir/code-style.mak
