.PHONY: all

all: clean
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Issue502.java
# TODO: This test is rather unstable, as Expected.txt relies on
# @SideEffectFree being constant #12().
	$(JAVAP) -v Issue502.class | grep "RuntimeVisibleAnnotations:" -A 2 > Out.txt 2>&1
	diff -u Expected.txt Out.txt

clean:
	rm -f Issue502.class Out.txt
