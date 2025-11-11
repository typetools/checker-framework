# This Makefile checks and enforces style for Python and Shell scripts.
# Most build system functionality exists in file `build.gradle`.

all default: style-check
	$(MAKE) -C .azure
	$(MAKE) -C checker/bin-devel

style-fix: python-style-fix shell-style-fix
style-check: asciidoc-style-check python-style-check python-typecheck shell-style-check


ASCIIDOC_FILES:=$(shell find . -name "*.adoc")
asciidoc-style-check:
	asciidoctor -o /dev/null ${ASCIIDOC_FILES}

PYTHON_FILES:=$(wildcard checker/bin-devel/*.py) $(wildcard docs/developer/release/*.py) $(shell grep -r -l --exclude='*.py' --exclude='*~' --exclude='#*' --exclude='*.tar' --exclude=gradlew --exclude-dir=.git --exclude-dir=.do-like-javac --exclude-dir=.html-tools --exclude-dir=.plume-scripts '^\#! \?\(/bin/\|/usr/bin/env \)python')
PYTHON_FILES_TO_CHECK:=$(filter-out ${lcb_runner},${PYTHON_FILES})
python-style-fix:
ifneq (${PYTHON_FILES},)
	@ruff --version
	@ruff format ${PYTHON_FILES_TO_CHECK}
	@ruff -q check ${PYTHON_FILES_TO_CHECK} --fix
endif
python-style-check:
ifneq (${PYTHON_FILES_TO_CHECK},)
	@ruff --version
	@ruff -q format --check ${PYTHON_FILES_TO_CHECK}
	@ruff -q check ${PYTHON_FILES_TO_CHECK}
endif
python-typecheck:
ifneq (${PYTHON_FILES_TO_CHECK},)
	@mypy --version
	@mypy --strict --install-types --non-interactive ${PYTHON_FILES_TO_CHECK} > /dev/null 2>&1 || true
	mypy --strict --ignore-missing-imports ${PYTHON_FILES_TO_CHECK}
endif


SH_SCRIPTS   := $(shell grep -r -l --exclude='*~' --exclude='#*' --exclude='*.tar' --exclude=gradlew --exclude-dir=.git --exclude-dir=.do-like-javac --exclude-dir .git-scripts --exclude-dir .html-tools --exclude-dir .plume-scripts '^\#! \?\(/bin/\|/usr/bin/env \)sh'   | grep -v addrfilter | grep -v cronic-orig | grep -v mail-stackoverflow.sh)
BASH_SCRIPTS := $(shell grep -r -l --exclude='*~' --exclude='#*'  --exclude='*.tar' --exclude=gradlew --exclude-dir=.git --exclude-dir=.do-like-javac --exclude-dir .git-scripts --exclude-dir .html-tools --exclude-dir .plume-scripts '^\#! \?\(/bin/\|/usr/bin/env \)bash' | grep -v addrfilter | grep -v cronic-orig | grep -v mail-stackoverflow.sh)
CHECKBASHISMS := $(shell if command -v checkbashisms > /dev/null ; then \
	  echo "checkbashisms" ; \
	else \
	  wget -q -N https://homes.cs.washington.edu/~mernst/software/checkbashisms && \
	  mv checkbashisms .checkbashisms && \
	  chmod +x ./.checkbashisms && \
	  echo "./.checkbashisms" ; \
	fi)
shell-style-fix:
ifneq ($(SH_SCRIPTS)$(BASH_SCRIPTS),)
	shfmt -w -i 2 -ci -bn -sr ${SH_SCRIPTS} ${BASH_SCRIPTS}
	shellcheck -x -P SCRIPTDIR --format=diff ${SH_SCRIPTS} ${BASH_SCRIPTS} | patch -p1
endif
shell-style-check:
ifneq ($(SH_SCRIPTS)$(BASH_SCRIPTS),)
	shfmt -d -i 2 -ci -bn -sr ${SH_SCRIPTS} ${BASH_SCRIPTS}
	shellcheck -x -P SCRIPTDIR --format=gcc ${SH_SCRIPTS} ${BASH_SCRIPTS}
endif
ifneq ($(SH_SCRIPTS),)
	${CHECKBASHISMS} -l ${SH_SCRIPTS}
endif

showvars:
	@echo "ASCIIDOC_FILES=${ASCIIDOC_FILES}"
	@echo "PYTHON_FILES=${PYTHON_FILES}"
	@echo "SH_SCRIPTS=${SH_SCRIPTS}"
	@echo "BASH_SCRIPTS=${BASH_SCRIPTS}"
