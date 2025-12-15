# System test of annotation tools

# This test runs most of the AFU tools, on a single program
# source-ann/AnnotationTest.java that contains an annotation in every
# possible location.

JAVA?=java
JAVAC?=javac

ANNCAT ?= ../../bin/anncat
JAVAVERSION := $(shell $(JAVA) -version 2>&1)

#Switch the lines below to deactivate this test
all: clean results
#all: warn_and_exit

warn_and_exit:
	@echo ""
	@echo "******************************************************"
	@echo "Warning: The sytem test has been temporarily disabled!"
	@echo "******************************************************"
	@echo ""

results: out1 check-out2 out3 out4.class check-out5

clean:
	rm -rf out[1-5]*


# Step 1: Compile the source with annotations.
out1:
	mkdir -p out1
	${JAVAC} -d out1 source-ann/AnnotationTest.java || (\rm -rf out1 && false)

# Step 2: Convert the annotated class file to an annotation file.
# Do we get the right annotations?
out2.jaif:
	CLASSPATH=`pwd`/out1 ${ANNCAT} --class out1/annotations/tests/AnnotationTest.class --out --index out2.jaif

out2.diff: out2.jaif
	# Ignore differences between bytecode indices between Java 8 and 9+
	# TODO: handle different versions?
	# diff -u expected-annos.jaif out2.jaif > out2.diff
	grep -v typecast expected-annos.jaif | grep -v instanceof | grep -v local > expected-annos.jaif.no-cast-no-instanceof
	grep -v typecast out2.jaif | grep -v instanceof | grep -v local > out2.jaif.no-cast-no-instanceof
	diff -u expected-annos.jaif.no-cast-no-instanceof out2.jaif.no-cast-no-instanceof > out2.diff
	rm -f expected-annos.jaif.no-cast-no-instanceof out2.jaif.no-cast-no-instanceof

.PHONY: check-out2
# Fail if out2.diff is non-empty
check-out2: out2.diff
	[ ! -s out2.diff ] || (cat out2.diff && false)

# Step 3: Compile the source without annotations.
out3:
	mkdir out3
	${JAVAC} -d out3 source-plain/AnnotationTest.java || (\rm -rf out3 && false)

# Step 4: Insert annotations into the class file.
# Can debug with:  javap -v out4.class
out4.class: out3
	CLASSPATH=`pwd`/out1 ${ANNCAT} --index expected-annos.jaif --out --class out3/annotations/tests/AnnotationTest.class --to out4.class

# Step 5: Convert the annotation-inserted class file to an annotation file.
# Do we get the right annotations?
# (The annotation-compiled and annotation-inserted class files tend to differ
# for stupid reasons (e.g., order of items in the constant pool), so we don't
# compare them.)
out5.jaif: out4.class
	CLASSPATH=`pwd`/out1 ${ANNCAT} --class out4.class --out --index out5.jaif

out5.diff: out5.jaif
	diff -u expected-annos.jaif out5.jaif > out5.diff

.PHONY: check-out5
# Fail if out5.diff is non-empty
check-out5: out5.diff
	[ ! -s out5.diff ] || (cat out5.diff && false)
