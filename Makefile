# This Makefile checks and enforces style.
# Most build system functionality exists in file `build.gradle`.

all default: style-check

# Code style; defines `style-check` and `style-fix`.
CODE_STYLE_EXCLUSIONS_USER := --exclude=manual.html --exclude=annotation-file-format.html
ifeq (,$(wildcard .plume-scripts))
ifeq (,$(wildcard checker/bin-devel/.plume-scripts))
dummy := $(shell ./gradlew -q getPlumeScripts)
endif
dummy := $(shell ln -s checker/bin-devel/.plume-scripts .plume-scripts)
endif
include .plume-scripts/code-style.mak
