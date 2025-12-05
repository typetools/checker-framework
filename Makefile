# This Makefile checks and enforces style for Python and Shell scripts.
# Most build system functionality exists in file `build.gradle`.

all default: style-check
	$(MAKE) -C .azure
	$(MAKE) -C checker/bin-devel

# Dependencies are defined below.
style-fix:
style-check:


style-fix: markdownlint-fix
style-check: markdownlint-check
markdownlint-fix:
	@markdownlint-cli2 --fix "**/*.md" "#node_modules"
markdownlint-check:
	@markdownlint-cli2 "**/*.md" "#node_modules"

style-fix: python-style-fix
style-check: python-style-check
# TODO: style-check: python-typecheck
PYTHON_FILES:=$(wildcard **/*.py) $(shell grep -r -l --exclude-dir=.do-like-javac --exclude-dir=.git --exclude-dir=.html-tools --exclude-dir=.plume-scripts --exclude-dir=.venv --exclude='*.py' --exclude='#*' --exclude='*~' --exclude='*.tar' --exclude=gradlew --exclude=lcb_runner '^\#! \?\(/bin/\|/usr/bin/\|/usr/bin/env \)python')
python-style-fix:
ifneq (${PYTHON_FILES},)
#	@uvx ruff --version
	@uvx ruff format ${PYTHON_FILES}
	@uvx ruff check ${PYTHON_FILES} --fix
endif
python-style-check:
ifneq (${PYTHON_FILES},)
	echo ${PATH}
	whereis uvx
	ls -al /
	ls -al /root
	ls -al /root/.local
	ls -al /root/.local/bin
	which uvx
	uvx ruff --version
	@uvx ruff format --check ${PYTHON_FILES}
	@uvx ruff check ${PYTHON_FILES}
endif
python-typecheck:
ifneq (${PYTHON_FILES},)
	@uv run ty check
endif
showvars::
	@echo "PYTHON_FILES=${PYTHON_FILES}"

style-fix: shell-style-fix
style-check: shell-style-check
SH_SCRIPTS   := $(shell grep -r -l --exclude-dir=.git --exclude-dir=.do-like-javac --exclude-dir .git-scripts --exclude-dir .html-tools --exclude-dir .plume-scripts --exclude='#*' --exclude='*~' --exclude='*.tar' --exclude=gradlew '^\#! \?\(/bin/\|/usr/bin/env \)sh'   | grep -v addrfilter | grep -v cronic-orig | grep -v mail-stackoverflow.sh | sort)
BASH_SCRIPTS := $(shell grep -r -l --exclude-dir=.git --exclude-dir=.do-like-javac --exclude-dir .git-scripts --exclude-dir .html-tools --exclude-dir .plume-scripts --exclude='#*' --exclude='*~'  --exclude='*.tar' --exclude=gradlew '^\#! \?\(/bin/\|/usr/bin/env \)bash' | grep -v addrfilter | grep -v cronic-orig | grep -v mail-stackoverflow.sh | sort)
CHECKBASHISMS := $(shell if command -v checkbashisms > /dev/null ; then \
	  echo "checkbashisms" ; \
	else \
	  wget -q -N https://homes.cs.washington.edu/~mernst/software/checkbashisms && \
	  mv checkbashisms .checkbashisms && \
	  chmod +x ./.checkbashisms && \
	  echo "./.checkbashisms" ; \
	fi)
SHFMT_EXISTS := $(shell command -v shfmt 2> /dev/null)
shell-style-fix:
ifneq ($(SH_SCRIPTS)$(BASH_SCRIPTS),)
ifdef SHFMT_EXISTS
	@shfmt -w -i 2 -ci -bn -sr ${SH_SCRIPTS} ${BASH_SCRIPTS}
endif
	@shellcheck -x -P SCRIPTDIR --format=diff ${SH_SCRIPTS} ${BASH_SCRIPTS} | patch -p1
endif
shell-style-check:
ifneq ($(SH_SCRIPTS)$(BASH_SCRIPTS),)
ifdef SHFMT_EXISTS
	@shfmt -d -i 2 -ci -bn -sr ${SH_SCRIPTS} ${BASH_SCRIPTS}
endif
	@shellcheck -x -P SCRIPTDIR --format=gcc ${SH_SCRIPTS} ${BASH_SCRIPTS}
endif
ifneq ($(SH_SCRIPTS),)
	@${CHECKBASHISMS} -l ${SH_SCRIPTS}
endif
showvars::
	@echo "SH_SCRIPTS=${SH_SCRIPTS}"
	@echo "BASH_SCRIPTS=${BASH_SCRIPTS}"
	@echo "CHECKBASHISMS=${CHECKBASHISMS}"
	@echo "SHFMT_EXISTS=${SHFMT_EXISTS}"
