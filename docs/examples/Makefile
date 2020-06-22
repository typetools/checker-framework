JAVAC ?= $(realpath ../../checker/bin/javac)
MAVEN_EXAMPLE=MavenExampleJDK11
ifeq ($(shell java -version 2>&1 | grep version | grep 1.8 > /dev/null; printf $$?), 0)
MAVEN_EXAMPLE=MavenExample
endif

.PHONY: all

all: compile

compile:
	$(JAVAC) *.java
	cd fenum-extension && $(MAKE)
	cd subtyping-extension && $(MAKE)
	cd units-extension && $(MAKE)
	cd ${MAVEN_EXAMPLE} && $(MAKE)
	cd lombok && $(MAKE)
	cd errorprone && $(MAKE)

# TODO: type check the different files with the right checker;
#   some tests expect errors, compare against expected errors.
