default: style-check

style-fix: python-style-fix shell-style-fix
style-check: asciidoc-style-check python-style-check shell-style-check


ASCIIDOC_FILES:=$(shell find . -name "*.adoc")
asciidoc-style-check:
	asciidoctor -o /dev/null ${ASCIIDOC_FILES}

PYTHON_FILES:=$(shell find . \( -name .do-like-javac -o -name .git-scripts \) -prune -o -name "*.py" -print)
install-ruff:
	@if ! command -v ruff ; then pipx install ruff ; fi
python-style-fix: install-ruff
	ruff --version
	ruff format ${PYTHON_FILES}
	ruff check ${PYTHON_FILES} --fix
python-style-check: install-ruff
	ruff --version
	ruff format --check ${PYTHON_FILES}
	ruff check ${PYTHON_FILES}


SH_SCRIPTS   := $(shell grep -r -l --exclude='*~' --exclude='*.tar' --exclude=gradlew --exclude-dir=.git --exclude-dir=.do-like-javac --exclude-dir .git-scripts --exclude-dir .html-tools --exclude-dir .plume-scripts '^\#! \?\(/bin/\|/usr/bin/env \)sh'   | grep -v addrfilter | grep -v cronic-orig | grep -v mail-stackoverflow.sh)
BASH_SCRIPTS := $(shell grep -r -l --exclude='*~' --exclude='*.tar' --exclude=gradlew --exclude-dir=.git --exclude-dir=.do-like-javac --exclude-dir .git-scripts --exclude-dir .html-tools --exclude-dir .plume-scripts '^\#! \?\(/bin/\|/usr/bin/env \)bash' | grep -v addrfilter | grep -v cronic-orig | grep -v mail-stackoverflow.sh)
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
