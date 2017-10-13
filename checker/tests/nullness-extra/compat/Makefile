.PHONY: all

all:
	rm -f Out.txt CompatTest.class lib/Lib.class javax/annotation/Nullable.class
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker lib/Lib.java javax/annotation/Nullable.java CompatTest.java > Out.txt 2>&1 || true
	diff -u Expected.txt Out.txt

# Additionally, we could test compiling lib and javax with a
# Java 7 compiler and then only compiling CompatTest.java with
# the Checker Framework.
# The result should be the same and I don't think we would
# learn anything additionally.
