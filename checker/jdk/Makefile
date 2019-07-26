# To use, run this command:
#   make all
# which creates the jdk.jar file.

# A version of the JDK with just headers/signatures: method bodies contain no bodies.
CTSYM ?= $(JAVA_HOME)/lib/ct.sym

CHECKER_DIRS = index lock nullness optional signedness

JAVA_FILES := $(shell find $(CHECKER_DIRS) -name '*.java' | sort)

jdk.jar: $(JAVA_FILES)
	$(MAKE) jdk.jar-unconditionally

# Remakes the annotated JDK, regardless of whether it is out of date.
jdk.jar-unconditionally: clean classes
# Extracts unannotated .class files from $(CTSYM), into files annotated/%.class.
	$(MAKE) unmodified
# Insert annotations in those files.
# The "jaifs" target:
#  * compiles the annotated JDK source code, then
#  * extracts the annotations from each .class file into a .jaif text file
#    (https://checkerframework.org/annotation-file-utilities/annotation-file-format.html)
# The "annotated" target inserts from .jaif files into the .class files extracted from ct.sym,
# in files annotated/%.class.
	$(MAKE) jaifs annotated
# Put those annotated .class files (originally from ct.sym) in a new jdk.jar file.
	$(MAKE) jdk.jar-insertion

# Does the final step of making jdk.jar.
# jdk.jar only contains those classes with annotations, not every class from
# the original JDK.
jdk.jar-insertion:
	echo Checker-Framework-Version: `git rev-parse --short HEAD` > manifest.txt
	echo Checker-Framework-Commit-Author: `git show -s --pretty=%aN\ \<%ae\> HEAD` >> manifest.txt
	echo Checker-Framework-Commit-Refs: `git show -s --pretty=%d HEAD` >> manifest.txt
	echo Creation-Date: `date` >> manifest.txt
	cd annotated; jar cfm ../jdk.jar ../manifest.txt .
	rm manifest.txt

# Helper

# All class files that contain an annotation, by directory
ANNOTATED_CLASSES_BY_DIR = $(shell find $(CHECKER_DIRS) -name '*.class' | sort)
# All class files that contain some annotation, possibly with duplicates.
# These, and only these, need to be extracted from $(CTSYM).
ANNOTATED_CLASSES = \
  $(patsubst index/build/%.class,annotated/%.class, \
  $(patsubst lock/build/%.class,annotated/%.class, \
  $(patsubst nullness/build/%.class,annotated/%.class, \
  $(patsubst optional/build/%.class,annotated/%.class, \
  $(patsubst signedness/build/%.class,annotated/%.class, \
  $(ANNOTATED_CLASSES_BY_DIR))))))

# Extract from $(CTSYM) an unannotated version of every file that will
# eventually get any annotation.
unmodified: $(ANNOTATED_CLASSES)

# Delegates
clean: $(addsuffix .clean, $(CHECKER_DIRS))
	rm -rf jdk.jar annotated

classes: $(addsuffix .classes, $(CHECKER_DIRS))

jaifs: $(addsuffix .jaifs, $(CHECKER_DIRS))

annotated: $(addsuffix .annotated, $(CHECKER_DIRS))

annotated/%.class:
	mkdir -p annotated
# Extract exactly one file from $(CTSYM)
# Ignore errors for now; the annotated JDK doesn't have the same inner classes as the real JDK.
	-cd annotated; unzip $(CTSYM) 'META-INF/sym/rt.jar/$*.class' > /dev/null
	-cp -R annotated/META-INF/sym/rt.jar/* annotated/
	rm -rf annotated/META-INF



# Helper targets to invoke subdir make files
# "$*" expands to the stem that matched the "%".
%.clean:
	cd $*; $(MAKE) clean

%.classes:
	cd $*; JDKDIR=$* $(MAKE) classes

# Uses the rules in file Makefile.jdk
%.jaifs:
	cd $*; $(MAKE) jaifs

%.annotated:
	cd $*; $(MAKE) annotated

TAGS: tags
tags:
	etags `find . -name '*.java'`
