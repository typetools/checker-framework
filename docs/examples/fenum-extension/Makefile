FILES=qual/MyFenum.java

JAVAOPTS=

JAVAC?=../../../checker/bin/javac

# gets the full path to the directory of the make file, which is also the root dir of the qual folder
# for custom projects, it is best to encode the full root path as a variable
PROJECTDIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

all: compile-for-test named-quals-test qual-folder-test clean

demo:
	$(JAVAC) $(JAVAOPTS) $(FILES)
	@echo "***** This command is expected to produce 21 errors:"
	$(JAVAC) -classpath $(PROJECTDIR) -processor org.checkerframework.checker.fenum.FenumChecker -AqualDirs=$(PROJECTDIR) FenumDemo.java

# compile qualifiers
compile-for-test:
	$(JAVAC) $(JAVAOPTS) $(FILES)

# test case for using externally defined qualifiers by explicitly naming them using the -Aquals option
named-quals-test:
	$(JAVAC) -classpath $(PROJECTDIR) -processor org.checkerframework.checker.fenum.FenumChecker -Aquals=qual.MyFenum FenumDemo.java > Out.txt 2>&1 || true
	diff -u Expected.txt Out.txt
	rm -f Out.txt

# test case for using externally defined qualifiers by loading them from a directory using the -AqualDirs option
qual-folder-test:
	$(JAVAC) -classpath $(PROJECTDIR) -processor org.checkerframework.checker.fenum.FenumChecker -AqualDirs=$(PROJECTDIR) FenumDemo.java > Out.txt 2>&1 || true
	diff -u Expected.txt Out.txt
	rm -f Out.txt

# test clean up
clean:
	rm -f qual/*.class
