# Very rough testing framework for the org.checkerframework.afu.annotator.  Running 'make all' will
# look for all myClass.goal files in this directory, run the annotator on the
# corresponding .jaif and .java files, and then output the difference in a
# myClass.diff file in this directory.
#
# To test just one file, use (for example) 'make myClass.diff'.

# Put user-specific changes in your own Makefile.user.
# Make will silently continue if that file does not exist.
-include ../Makefile.user

# Override these in Makefile.user if the java and javac commands are not on
# your execution path.  Example from Makefile.user:
#   JAVA=${JAVA_HOME}/bin/java
#   JAVAC=${JAVA_HOME}/bin/javac
JAVA?=java
JAVAC?=javac

# Need --add-opens to access CommandLine.parse dynamically to check its type.
export JAVA:=$(JAVA) --add-opens jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED  --add-opens jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED

export SHELL=/bin/bash -o pipefail


DIFFS := $(wildcard *.goal)
DISABLED := $(shell grep -le "@skip-test" $(DIFFS))
FILTERED := $(filter-out $(DISABLED),$(DIFFS))
DIFFS := $(patsubst %.goal, %.diff, $(FILTERED))
SRC := $(patsubst %.goal, %.java, $(FILTERED))
OUT := $(patsubst %.goal, %.output, $(FILTERED))

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
	@$(JAVA) -cp bin:../bin VerifyDiffs --show_all

# Remakes the little java program that checks and compares diffs
../bin/VerifyDiffs.class : ../VerifyDiffs.java
	@$(JAVAC) -g -cp ../../build/classes/java/maincompile -d ../bin ../VerifyDiffs.java

# Compiles all the test cases (be verbose about this).
.PHONY: compile
compile : $(SRC)
	mkdir -p bin
	$(JAVAC) -g -cp ../../build/classes/java/main -d bin $(SRC)

# Compiles just one test case
.PRECIOUS : bin/annotator/tests/%.class
bin/annotator/tests/%.class: %.java
	mkdir -p bin
	$(JAVAC) -g -cp bin:../../build/classes/java/main -d bin -sourcepath . $<

# Actually runs the annotator to create the annotated java file.
.PRECIOUS: %.output
%.output: %.jaif %.java bin/annotator/tests/%.class ../../build/classes/java/main ../../annotation-file-utilities-all.jar
	$(JAVA) \
	-cp ../../annotation-file-utilities-all.jar:bin \
	org.checkerframework.afu.annotator.Main \
	${DEBUG} \
	--abbreviate=false \
	-d $*-output \
	$*.jaif \
	$*.java \
	2>&1 | tee $*.log
	find "$*-output" -name '*.java' -print | xargs cat > "$*.output"
	rm -rf $*-output

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
