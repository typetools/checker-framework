# this makefile contains the commands to test the AnnotationClassLoader
# it uses the aliasing checker as it is one of the framework checkers that has a qual directory with qualifiers that must be loaded using the loader

# when called by the ant tests, JAVAC is defined as $JAVA_HOME/bin/javac, which could be jdk8 or jdk7
# when executed as a demo, we'll use openjdk 8 located in the following directory (change to your setup):
# ?= sets JAVAC only if JAVAC is undefined
JAVAC ?= /usr/lib/jvm/java-8-openjdk-amd64/bin/javac

# gets the full path to the directory of the make file, which is also the root dir of the qual folder
# for custom projects, it is best to encode the full root path as a variable
PROJECTDIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

FRAMEWORKJAR := $(PROJECTDIR)/../../dist/framework.jar

# when called by the ant tests, JAVACJAR is defined as ${jsr308.langtools.dist}/lib/javac.jar
# when executed as a demo, we'll use javac.jar located in a directory structure which assumes that the jsr308-langtools project is located in the same directory as checker-framework (change to your setup):
# ?= sets JAVACJAR only if JAVACJAR is undefined
JAVACJAR ?= $(PROJECTDIR)/../../../../jsr308-langtools/dist/lib/javac.jar

# build directories
DATAFLOWBUILD := $(PROJECTDIR)/../../../dataflow/build/
JAVACUTILBUILD := $(PROJECTDIR)/../../../javacutil/build/
STUBPARSERBUILD := $(PROJECTDIR)/../../../stubparser/build/
FRAMEWORKBUILD := $(PROJECTDIR)/../../build/

all: load-from-dir-test load-from-jar-test

# ======================================================
# demo or manual test usage:
# loads from build directories
demo1:
	@echo "***** This command is expected to produce an error on line 7:"
	$(JAVAC) \
	-J-Xbootclasspath/p:$(DATAFLOWBUILD):$(JAVACUTILBUILD):$(STUBPARSERBUILD):$(FRAMEWORKBUILD):$(JAVACJAR):$(PROJECTDIR) \
	-processor org.checkerframework.common.aliasing.AliasingChecker \
	-Anomsgtext \
	-AprintErrorStack \
	-Astubs=$(PROJECTDIR)/../aliasing/stubfile.astub \
	LoaderTest.java

# loads from framework.jar
demo2:
	@echo "***** This command is expected to produce an error on line 7:"
	$(JAVAC) \
	-J-Xbootclasspath/p:$(FRAMEWORKJAR):$(JAVACJAR):$(PROJECTDIR) \
	-processor org.checkerframework.common.aliasing.AliasingChecker \
	-Anomsgtext \
	-AprintErrorStack \
	-Astubs=$(PROJECTDIR)/../aliasing/stubfile.astub \
	LoaderTest.java

# ======================================================
# ant test usage
# loads from build directories
load-from-dir-test:
	-$(JAVAC) -J-Xbootclasspath/p:$(DATAFLOWBUILD):$(JAVACUTILBUILD):$(STUBPARSERBUILD):$(FRAMEWORKBUILD):$(JAVACJAR):$(PROJECTDIR) -processor org.checkerframework.common.aliasing.AliasingChecker -Anomsgtext -AprintErrorStack -Astubs=$(PROJECTDIR)/../aliasing/stubfile.astub LoaderTest.java > Out.txt 2>&1
	diff -u Out.txt Expected.txt
	-rm Out.txt

#loads from framework.jar
load-from-jar-test:
	-$(JAVAC) -J-Xbootclasspath/p:$(FRAMEWORKJAR):$(JAVACJAR):$(PROJECTDIR) -processor org.checkerframework.common.aliasing.AliasingChecker -Anomsgtext -AprintErrorStack -Astubs=$(PROJECTDIR)/../aliasing/stubfile.astub LoaderTest.java > Out.txt 2>&1
	diff -u Out.txt Expected.txt
	-rm Out.txt
