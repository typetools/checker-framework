.PHONY: all

all:
	$(JAVAC) -processor regex,org.checkerframework.checker.tainting.TaintingChecker TwoCheckers.java 2>&1 | grep -v "WARNING: The Checker Framework is only tested with" > out.txt || true
	diff -u expected.txt out.txt
