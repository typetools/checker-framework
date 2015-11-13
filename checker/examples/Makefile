.PHONY: all

all: compile

compile:
	$(JAVAC) *.java
	cd units-extension && $(MAKE)

# TODO: type check the different files with the right checker;
#   some tests expect errors, compare against expected errors.
