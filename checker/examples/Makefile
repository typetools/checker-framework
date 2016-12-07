JAVAC ?= $(realpath ../bin/javac)

.PHONY: all

all: compile

compile:
	$(JAVAC) *.java
	cd fenum-extension && $(MAKE)
	cd subtyping-extension && $(MAKE)
	cd units-extension && $(MAKE)

# TODO: type check the different files with the right checker;
#   some tests expect errors, compare against expected errors.
