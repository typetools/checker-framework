JAVAC ?= $(realpath ../../checker/bin/javac)

.PHONY: all

all: compile

compile:
	$(JAVAC) *.java
	$(MAKE) -C fenum-extension
	$(MAKE) -C subtyping-extension
	$(MAKE) -C units-extension
# The Maven example downloads a lot.  Try twice in case of network lossage.
	$(MAKE) -C MavenExample || (sleep 60 && echo "Trying again:" && $(MAKE) -C MavenExample)
# As of 2026-03-11 (Lombok 1.18.44), delombok does not support JDK 26.
	JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//') && \
	if [ "${JAVA_VER}" -lt "26" ]; then $(MAKE) -C lombok; fi
	$(MAKE) -C errorprone

# TODO: type check the different files with the right checker;
#   some tests expect errors, compare against expected errors.
