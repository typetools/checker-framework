.PHONY: all

all: clean
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Issue502.java
# TODO: This test is rather unstable, as Expected.txt relies on
# @SideEffectFree being constant #12().
	$(JAVAP) -v Issue502 | grep "RuntimeVisibleAnnotations:" -A 2 > Out.txt 2>&1
	diff -u Out.txt Expected.txt

clean:
	-rm Issue502.class Out.txt
