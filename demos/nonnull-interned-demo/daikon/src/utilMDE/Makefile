### User customizations

# Put user-specific changes in your own Makefile.user
#  (such as changes to UTILMDE_DIR or JAVAC).
# Make will silently continue if file does not exist.
-include Makefile.user

JAVAC ?= javac
# Removed "-J-Xmx800m", because it is incompatible with JDK 7
JAVAC_ARGS ?= -g -target 5 -Xlint -Xmaxwarns 1000
JAVAC_COMMAND = ${JAVAC} ${JAVAC_ARGS}
# JAVAC ?= javac -g -Xlint:unchecked
# JAVAC ?= jikes -g +E +F
JAVA ?= java -ea

JAVA_FILES := $(shell find . \( -name CVS \) -prune -o -name '*.java' -print)
JAVA_LINT_FILES := $(shell find . \( -name CVS -o -name ArraysMDE.java -o -name WeakHasherMap.java -o -name Intern.java -o -name OrderedPairIterator.java -o -name WeakIdentityHashMap.java \) -prune -o -name '*.java' -print)
JAVA_CPP_FILES := $(shell find . \( -name CVS \) -prune -o -name '*.java.jpp' -print)
ALL_JAVA_FILES := ${JAVA_FILES} ${JAVA_CPP_FILES}
AUTO_GENERATED_FILES = \
	MathMDE.java \
	LimitedSizeIntSet.java

default: all

all: all_stop_on_error

# Don't bother to run the unit tests if the compilation failed.
all_stop_on_error:
	$(MAKE) --stop all_notest test

all_javac: all_javac_notest test

all_notest:  ${AUTO_GENERATED_FILES}
	${JAVAC_COMMAND} ${JAVAC_EXTRA_ARGS} ${JAVA_FILES} 2>&1 | ${DAIKONBIN}/javac-xlint-prune.pl

clean:
	rm -f *.class

very_clean: clean
	rm -f ${AUTO_GENERATED_FILES}

tags:
	etags ${ALL_JAVA_FILES}

# This "set" form doesn't seem to work for me (maybe needs to be export?).
# Used to depend on targets "clean all", but I want to run this in "all".
test:
	set JAVA_COMPILER=NONE; ${JAVA} utilMDE.TestUtilMDE

doc: javadoc

javadoc: $(JAVA_FILES)
	mkdir -p doc
	javadoc -d doc utilMDE
	# makes "doc" (the target of this rule) look up to date
	touch doc
	# jdk-add-munged-anchor-names doc/utilMDE/*.html
	@echo "Optionally run jdk-index-to-alist (for Emacs support)"

audit:
	(cd ..; maudit utilMDE)

# No need to do "make doc" or "make all" before running this.
# (This includes .class files, so users don't need to recompile.)
utilMDE.tar.gz: $(ALL_JAVA_FILES) Makefile README
	rm -rf /tmp/utilMDE
	mkdir /tmp/utilMDE
	cp -p $(ALL_JAVA_FILES) /tmp/utilMDE
	cp -p Makefile README /tmp/utilMDE
	(cd /tmp/utilMDE; make JAVAC_EXTRA_ARGS='-classpath .:${DAIKONDIR}/java/lib/junit.jar:${DAIKONDIR}/java/lib/checkers.jar:${DAIKONDIR}/java/lib/bcel.jar:$(JDKDIR)/lib/tools.jar' all_notest doc)
	(cd /tmp; tar zcf utilMDE.tar.gz utilMDE)
	rm -f utilMDE.tar.gz
	mv /tmp/utilMDE.tar.gz .
	rm -rf /tmp/utilMDE

showvars:
	@echo ALL_JAVA_FILES = $(ALL_JAVA_FILES)
	@echo JAVA_FILES = $(JAVA_FILES)
	@echo JAVA_LINT_FILES = $(JAVA_LINT_FILES)
	@echo JAVA_CPP_FILES = $(JAVA_CPP_FILES)


###########################################################################
### Automatically generated files
###

# JAVA_CPP = java-cpp.pl $< -DDEFINEDVAR > $@
# This rule does not work with make 3.77, but does work with make 3.78.
JAVA_CPP ?= (rm -f $@; java-cpp.pl -DDEFINEDVAR $< 2>&1 > $@ && chmod -w $@)

MathMDE.java: MathMDE.java.jpp MathMDE-gcd.java.jpp MathMDE-modulus.java.jpp MathMDE-nonmodulus.java.jpp MathMDE-help.java.jpp MathMDE-helpend.java.jpp
	$(JAVA_CPP)

LimitedSizeIntSet.java: LimitedSizeSet.java.jpp
	$(JAVA_CPP:DEFINEDVAR=TYPEINT)


###########################################################################
###
###

# Existence of this rule means that a target is deleted if has just changed
# and its rule commands exit with nonzero status.  It's needed for the
# java-cpp rules above.
# (Actually, it isn't anymore, for I arrange not to overwrite the file
# unless success.  But leave it in for now anyway.)

.DELETE_ON_ERROR:

# end
