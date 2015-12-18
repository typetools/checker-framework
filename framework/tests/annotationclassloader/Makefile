# this makefile contains the commands to test the AnnotationClassLoader
# it uses the aliasing checker as it is one of the framework checkers that has a qual directory with qualifiers that must be loaded using the loader

JAVAOPTS=-source 7 -target 7

# when called by the ant tests, JAVAC is defined as $JAVA_HOME/bin/javac, which could be jdk8 or jdk7
# when executed as a demo, we'll use openjdk 8
ifndef JAVAC
	JAVAC?=/usr/lib/jvm/java-8-openjdk-amd64/bin/javac
endif

# gets the full path to the directory of the make file, which is also the root dir of the qual folder
# for custom projects, it is best to encode the full root path as a variable
PROJECTDIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

FRAMEWORKJAR := $(PROJECTDIR)/../../dist/framework.jar
JAVACJAR := $(PROJECTDIR)/../../../../jsr308-langtools/dist/lib/javac.jar

DATAFLOWBUILD := $(PROJECTDIR)/../../../dataflow/build/
JAVACUTILBUILD := $(PROJECTDIR)/../../../javacutil/build/
STUBPARSERBUILD := $(PROJECTDIR)/../../../stubparser/build/
FRAMEWORKBUILD := $(PROJECTDIR)/../../build/

all: load-from-dir-test load-from-jar-test

demo: demo1 demo2

# loads from framework.jar
demo1:
	$(JAVAC) \
	-J-Xbootclasspath/p:$(FRAMEWORKJAR):$(JAVACJAR):$(PROJECTDIR) \
	-processor org.checkerframework.common.aliasing.AliasingChecker \
	-Anomsgtext \
	-AprintErrorStack \
	-Astubs=$(PROJECTDIR)/../aliasing/stubfile.astub \
	LoaderTest.java

# loads from framework/build
demo2:
	$(JAVAC) \
	-J-Xbootclasspath/p:$(DATAFLOWBUILD):$(JAVACUTILBUILD):$(STUBPARSERBUILD):$(FRAMEWORKBUILD):$(JAVACJAR):$(PROJECTDIR) \
	-processor org.checkerframework.common.aliasing.AliasingChecker \
	-Anomsgtext \
	-AprintErrorStack \
	-Astubs=$(PROJECTDIR)/../aliasing/stubfile.astub \
	LoaderTest.java

load-from-dir-test:
	-$(JAVAC) -J-Xbootclasspath/p:$(DATAFLOWBUILD):$(JAVACUTILBUILD):$(STUBPARSERBUILD):$(FRAMEWORKBUILD):$(JAVACJAR):$(PROJECTDIR) -processor org.checkerframework.common.aliasing.AliasingChecker -Anomsgtext -AprintErrorStack -Astubs=$(PROJECTDIR)/../aliasing/stubfile.astub LoaderTest.java > Out.txt 2>&1
	diff -u Out.txt Expected.txt
	-rm Out.txt

load-from-jar-test:
	-$(JAVAC) -J-Xbootclasspath/p:$(FRAMEWORKJAR):$(JAVACJAR):$(PROJECTDIR) -processor org.checkerframework.common.aliasing.AliasingChecker -Anomsgtext -AprintErrorStack -Astubs=$(PROJECTDIR)/../aliasing/stubfile.astub LoaderTest.java > Out.txt 2>&1
	diff -u Out.txt Expected.txt
	-rm Out.txt
