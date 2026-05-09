# This Makefile checks and enforces style.
# Most build system functionality exists in file `build.gradle`.

all default: style-check

PLUME_SCRIPTS=checker/bin-devel/.plume-scripts

# Code style; defines `style-check` and `style-fix`.
CODE_STYLE_EXCLUSIONS_USER := --exclude=manual.html --exclude=annotation-file-format.html --exclude=${PLUME_SCRIPTS} --exclude=checker/bin-devel/.git-scripts --exclude=checker/bin-devel/.html-tools
ifeq (,$(wildcard ${PLUME_SCRIPTS}))
dummy := $(shell ./gradlew -q getPlumeScripts)
endif
include ${PLUME_SCRIPTS}/code-style.mak

.PHONY: get-plume-scripts
get-plume-scripts:
