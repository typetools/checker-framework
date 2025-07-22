# Very rough testing framework for the org.checkerframework.afu.annotator.  Running 'make all' will
# look for all myClass.goal files in this directory, run the annotator on the
# corresponding .jaif and .java files, and then output the difference in a
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
export JAVA:=$(JAVA) --add-opens jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED  --add-opens jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-opens jdk.internal.opt/jdk.internal.opt=ALL-UNNAMED

export SHELL=/bin/bash -o pipefail


DIFFS := $(sort $(wildcard *.goal))
DISABLED := $(shell grep -le "@skip-test" $(DIFFS))
FILTERED := $(filter-out $(DISABLED),$(DIFFS))
DIFFS := $(patsubst %.goal, %.diff, $(FILTERED))

DEBUG :=
# Use this to enable some debugging.
# DEBUG := --debug

default : all

.PHONY: all
all : $(DIFFS) abbreviated converted enum-imports ad-hoc issue155 system-test source-extension showdiffs results

.PHONY: abbreviated
abbreviated:
	${MAKE} -C abbreviated

.PHONY: converted
converted:
	${MAKE} -C converted

.PHONY: enum-imports
enum-imports:
	${MAKE} -C enum-imports

.PHONY: ad-hoc
ad-hoc:
	${MAKE} -C ad-hoc

.PHONY: issue155
issue155:
	${MAKE} -C issue155

.PHONY: source-extension
source-extension:
	${MAKE} -C source-extension

.PHONY: system-test
system-test:
	${MAKE} -C system-test

# Display results (OK or FAILED) for all .diff files.
.PHONY: results
results: bin/VerifyDiffs.class
	@echo ""
	@echo "=== RESULTS ==="
	@echo ""
	@$(JAVA) -cp bin VerifyDiffs --show_all

# Show all .diff files; good for running under CI.
.PHONY: showdiffs
showdiffs: bin/VerifyDiffs.class
	cat *.diff

# Remakes the little Java program that checks and compares diffs.
bin/VerifyDiffs.class : VerifyDiffs.java
	@$(JAVAC) -g -cp ../build/classes/java/maincompile -d bin VerifyDiffs.java

# Compiles all the test cases (be verbose about this).
compile :
	mkdir -p bin
	$(JAVAC) -g -cp ../build/classes/java/main -d bin *.java

.PRECIOUS : bin/annotator/tests/%.class
bin/annotator/tests/%.class: %.java
	mkdir -p bin
# Added "-Xlint:-options" to see if it permits Jenkins job to succeed, due to
# problem "target value 1.8 is obsolete and will be removed in a future release"
	$(JAVAC) -Xlint:-options -g -cp bin:../dist/annotation-file-utilities-all.jar -d bin -sourcepath . $*.java

# Actually runs the annotator to create the annotated java file.
.PRECIOUS: %.output
%.output: %.jaif %.java bin/annotator/tests/%.class ../build/classes/java/main ../dist/annotation-file-utilities-all.jar
	$(JAVA) \
	-cp ../dist/annotation-file-utilities-all.jar:bin \
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
	(cd abbreviated && make clean)
	(cd converted && make clean)
	(cd enum-imports && make clean)
	(cd ad-hoc && make clean)
	(cd issue155 && make clean)
	(cd source-extension && make clean)
	(cd system-test && make clean)
