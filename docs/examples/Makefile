JAVAC ?= $(realpath ../../checker/bin/javac)

.PHONY: all

all: compile

compile:
	$(JAVAC) *.java
	cd fenum-extension && $(MAKE)
	cd subtyping-extension && $(MAKE)
	cd units-extension && $(MAKE)
# The Maven example downloads a lot.  Try twice in case of network lossage.
	cd MavenExample && ($(MAKE) || (sleep 60 && echo "Trying again:" && $(MAKE)))
	cd lombok && $(MAKE)
	cd errorprone && $(MAKE)

# TODO: type check the different files with the right checker;
#   some tests expect errors, compare against expected errors.
