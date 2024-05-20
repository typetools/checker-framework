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
	$(MAKE) -C lombok
	$(MAKE) -C errorprone

# TODO: type check the different files with the right checker;
#   some tests expect errors, compare against expected errors.
