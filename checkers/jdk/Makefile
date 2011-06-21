# To use, run this command:
#   make all
# which creates the jdk.jar file.

RTJAR ?= $(JAVA_HOME)/jre/lib/rt.jar

CHECKER_DIRS = nullness igj javari

all: clean classes
# Extracts unannotated .class files from $(RTJAR)
	$(MAKE) unmodified
# Insert annotations in those files.
	$(MAKE) jaifs annotated
# Put those annotated .class files back in jdk.jar.
	$(MAKE) jdk.jar

## DO NOT INVOKE DIRECTLY.  USE "make all" INSTEAD.
# jdk.jar only contains those classes with annotations, not every class from
# the original JDK.
jdk.jar:
	cd annotated; jar cf ../jdk.jar .

# Helper

# All class files that contain an annotation, by directory
ANNOTATED_CLASSES_BY_DIR = $(shell find $(CHECKER_DIRS) -name '*.class')
# All class files that contain some annotation, possibly with duplicates.
# These, and only these, need to be extracted from $(RTJAR).
ANNOTATED_CLASSES = \
	$(patsubst nullness/build/%.class,annotated/%.class, \
	  $(patsubst igj/build/%.class,annotated/%.class, \
	    $(patsubst javari/build/%.class,annotated/%.class, \
		  $(ANNOTATED_CLASSES_BY_DIR))))

# Javarifier seems to only check the first .jar it gets on the
# -soot-classpath (which will be this one), so if these are missing from
# it, Javarifier issues the error "Missing stub for class
# java.lang.annotation.Annotation".
JAVARIFIER_REQUIRED = \
	annotated/java/lang/annotation/Annotation.class \
	annotated/java/lang/annotation/Documented.class \
	annotated/java/lang/annotation/Retention.class \
	annotated/java/lang/annotation/Target.class

# Extract from $(RTJAR) an unannotated version of every file that will
# eventually get any annotation.
unmodified: $(ANNOTATED_CLASSES) $(JAVARIFIER_REQUIRED)

# Delegates
clean: $(addsuffix .clean, $(CHECKER_DIRS))
	rm -rf jdk.jar annotated

classes: $(addsuffix .classes, $(CHECKER_DIRS))

jaifs: $(addsuffix .jaifs, $(CHECKER_DIRS))

annotated: $(addsuffix .annotated, $(CHECKER_DIRS))

annotated/%.class:
	mkdir -p annotated
# Ignore errors for now; we get errors when building the JDK for Java 6 but looking for files that are in the annotated JDK because of Java 7.
	-cd annotated; unzip $(RTJAR) '$*.class' > /dev/null

# Helper targets to invoke subdir make files
# "$*" expands to the stem that matched the "%".
%.clean:
	cd $*; $(MAKE) clean

%.classes:
	cd $*; $(MAKE) classes

%.jaifs:
	cd $*; $(MAKE) jaifs

%.annotated:
	cd $*; $(MAKE) annotated
