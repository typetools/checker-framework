# Very rough testing framework for the org.checkerframework.afu.annotator.  Running 'make all' will
# look for all myClass.goal files in the parent directory, run the annotator on the
# corresponding .jaif in this directory and the .java file in the parent directory,
# and then output the difference in a
# myClass.diff file in this directory.
#
# To test just one file, use (for example) 'make myClass.diff'.

# Put user-specific changes in your own Makefile.user.
# Make will silently continue if that file does not exist.
-include Makefile.user

# Override these in Makefile.user if the java and javac commands are not on
# your execution path.  Example from Makefile.user:
#   JAVA=${JAVA_HOME}/bin/java
#   JAVAC=${JAVA_HOME}/bin/javac
export JAVA?=java -ea
export JAVAC?=javac

# Need --add-opens to access CommandLine.parse dynamically to check its type.
export JAVA:=$(JAVA) --add-opens jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED  --add-opens jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED

export SHELL=/bin/bash -o pipefail


# Use .jaif instead of .goal (which ../Makefile uses), because only
# .jaif files are checked in this directory.
DIFFS := $(sort $(wildcard *.jaif))
DISABLED := $(shell grep -le "@skip-test" $(DIFFS))
FILTERED := $(filter-out $(DISABLED),$(DIFFS))
DIFFS := $(patsubst %.jaif, %.diff, $(FILTERED))

DEBUG :=
# Use this to enable some debugging.
# DEBUG := --debug

default : all

.PHONY: all
all : $(DIFFS) results

# Display results of all .diff files.
.PHONY: results
results: ../bin/VerifyDiffs.class
	@echo ""
	@echo "=== RESULTS ==="
	@echo ""
	@$(JAVA) -cp ../bin VerifyDiffs --show_all

# Remakes the little Java program that checks and compares diffs.
../bin/VerifyDiffs.class : ../VerifyDiffs.java
	@$(JAVAC) -g -cp ../../build/classes/java/maincompile -d ../bin ../VerifyDiffs.java

# Compiles all the test cases (be verbose about this).
compile :
	mkdir -p bin
	$(JAVAC) -g -cp ../../build/classes/java/main -d bin ../*.java

.PRECIOUS : bin/annotator/tests/%.class
bin/annotator/tests/%.class: %.java
	mkdir -p bin
# Added "-Xlint:-options" to see if it permits Jenkins job to succeed, due to
# problem "target value 1.8 is obsolete and will be removed in a future release"
	$(JAVAC) -Xlint:-options -g -cp bin:../dist/annotation-file-utilities.jar -d bin -sourcepath . $*.java

# Create a symbolic link for a .java file.
.PRECIOUS: %.java
%.java:
	rm -f $*.java
	ln -s ../$*.java $*.java

# Create a symbolic link for a .goal file.
.PRECIOUS: %.goal
%.goal:
	rm -f $*.goal
	ln -s ../$*.goal $*.goal

# Actually runs the annotator to create the annotated java file.
.PRECIOUS: %.output
# TODO: added `-` to these commands to proceed past failures
# The failures will still show up in the summary, but it might
# be better to fail earlier.
%.output: %.jaif %.java bin/annotator/tests/%.class ../../build/classes/java/main ../../dist/annotation-file-utilities-all.jar
	-$(JAVA) \
	    -cp ../../dist/annotation-file-utilities-all.jar:bin \
	    org.checkerframework.afu.annotator.Main \
	    ${DEBUG} \
	    --abbreviate=false \
	    -d $*-output \
	    $*.jaif \
	    $*.java \
	    2>&1 | tee $*.log
	-find "$*-output" -name '*.java' -print | xargs cat > "$*.output"
	-rm -rf $*-output

# Compare the output of the annotator and the goal file.
%.diff: %.goal %.output
	-diff -u $*.goal $*.output 2>&1 | tee $*.diff

# Remove all .diff, .log files from the tests directory.
.PHONY: clean
clean :
	rm -rf bin
	rm -f *.diff
	rm -f *.log
	rm -f *.output
	rm -f *.java
	rm -f *.goal
