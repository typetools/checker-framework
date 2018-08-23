FILES=qual/Frequency.java \
  qual/FrequencyRelations.java \
  qual/Hz.java \
  qual/kHz.java

JAVAOPTS= -classpath .:../../../checker/dist/checker.jar

JAVAC?=../../../checker/bin/javac

# gets the full path to the directory of the make file, which is also the root dir of the qual folder
# for custom projects, it is best to encode the full root path as a variable
PROJECTDIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

all: compile-for-test named-quals-test qual-folder-test clean

demo:
	$(JAVAC) $(JAVAOPTS) $(FILES)
	@echo "***** This command is expected to produce errors on line 15 & 66:"
	$(JAVAC) -classpath $(PROJECTDIR) -processor org.checkerframework.checker.units.UnitsChecker -AunitsDirs=$(PROJECTDIR) Demo.java

# compile qualifiers
compile-for-test:
	$(JAVAC) $(JAVAOPTS) $(FILES)

# test case for using externally defined units by explicitly naming them using the -Aunits option
named-quals-test:
	$(JAVAC) -classpath $(PROJECTDIR) -processor org.checkerframework.checker.units.UnitsChecker -Aunits=qual.Hz,qual.kHz,qual.Frequency Demo.java > Out.txt 2>&1 || true
	diff -u Expected.txt Out.txt
	rm -f Out.txt

# test case for using externally defined units by loading them from a directory using the -AunitsDirs option
qual-folder-test:
	$(JAVAC) -classpath $(PROJECTDIR) -processor org.checkerframework.checker.units.UnitsChecker -AunitsDirs=$(PROJECTDIR) Demo.java > Out.txt 2>&1 || true
	diff -u Expected.txt Out.txt
	rm -f Out.txt

# test clean up
clean:
	rm -f qual/*.class
