# This Makefile checks and enforces style.
# Most build system functionality exists in file `build.gradle`.

all default: style-check

# Code style; defines `style-check` and `style-fix`.
PLUME_SCRIPTS := checker/bin-devel/.plume-scripts
ifeq (,$(wildcard ${PLUME_SCRIPTS}))
dummy := $(shell git clone --depth=1 -q https://github.com/plume-lib/plume-scripts.git ${PLUME_SCRIPTS})
endif
include ${PLUME_SCRIPTS}/code-style.mak
