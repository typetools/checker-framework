# This makefile contains the commands to test the AnnotationClassLoader.
# Tt uses the aliasing checker as it is one of the framework checkers that has a
# qual directory with qualifiers that must be loaded using the loader.

# Gets the full path to the directory of the make file, which is also the root
# directory of the qual folder.
# For custom projects, it is best to encode the full root path as a variable.
PROJECTDIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

# Use the Checker Framework javac.
# TODO: the dependency on the checker project isn't nice.
JAVAC = $(PROJECTDIR)/../../../checker/bin/javac

FRAMEWORKJAR := $(PROJECTDIR)/../../dist/framework.jar

STUBPARSERJAR := $(PROJECTDIR)/../../../../stubparser/javaparser-core/target/stubparser.jar

CHECKERQUALJAR := $(PROJECTDIR)/../../../checker/dist/checker-qual.jar

# build directories
DATAFLOWBUILD := $(PROJECTDIR)/../../../dataflow/build/classes/java/main
JAVACUTILBUILD := $(PROJECTDIR)/../../../javacutil/build/classes/java/main
FRAMEWORKBUILD := $(PROJECTDIR)/../../build/classes/java/main:$(PROJECTDIR)/../../build/resources/main/
CHECKERQUALBUILD := $(PROJECTDIR)/../../../checker-qual/build/classes/java/main


all: load-from-dir-test load-from-jar-test

# ======================================================
# demo or manual test usage:
# loads from build directories
demo1:
	@echo "***** This command is expected to produce an error on line 7:"
	$(JAVAC) \
	  -processorpath $(DATAFLOWBUILD):$(JAVACUTILBUILD):$(FRAMEWORKBUILD):${STUBPARSERJAR} \
	  -classpath $(PROJECTDIR):${CHECKERQUALBUILD} \
	  -processor org.checkerframework.common.aliasing.AliasingChecker \
	  -Anomsgtext \
	  -ApermitMissingJdk \
	  LoaderTest.java

# loads from framework.jar
demo2:
	@echo "***** This command is expected to produce an error on line 7:"
	$(JAVAC) \
	  -processorpath $(FRAMEWORKJAR) \
	  -classpath $(PROJECTDIR):${CHECKERQUALJAR} \
	  -processor org.checkerframework.common.aliasing.AliasingChecker \
	  -Anomsgtext \
	  -ApermitMissingJdk \
	  LoaderTest.java

# ======================================================
# gradle test usage:
# loads from build directories
load-from-dir-test:
	$(JAVAC) \
	  -processorpath $(DATAFLOWBUILD):$(JAVACUTILBUILD):$(FRAMEWORKBUILD):${STUBPARSERJAR} \
	  -classpath $(PROJECTDIR):${CHECKERQUALBUILD} \
	  -processor org.checkerframework.common.aliasing.AliasingChecker \
	  -Anomsgtext \
	  -ApermitMissingJdk \
	  LoaderTest.java > Out.txt 2>&1 || true
	diff -u Expected.txt Out.txt
	rm -f Out.txt

# loads from framework.jar
load-from-jar-test:
	$(JAVAC) \
	  -processorpath $(FRAMEWORKJAR) \
	  -classpath $(PROJECTDIR):${CHECKERQUALJAR} \
	  -processor org.checkerframework.common.aliasing.AliasingChecker \
	  -Anomsgtext \
	  -ApermitMissingJdk \
	  LoaderTest.java > Out.txt 2>&1 || true
	diff -u Expected.txt Out.txt
	rm -f Out.txt
