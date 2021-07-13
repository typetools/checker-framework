.PHONY: all

all:
	$(JAVAC) -processor regex,org.checkerframework.checker.tainting.TaintingChecker TwoCheckers.java 2>&1 | grep -v 'Option --illegal-access is deprecated' > out.txt || true
	diff -u expected.txt out.txt
