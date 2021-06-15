.PHONY: all

all:
	$(JAVAC) -processor regex,org.checkerframework.checker.tainting.TaintingChecker TwoCheckers.java | grep -v 'Option --illegal-access is deprecated' > out.txt 2>&1 || true
	diff -u expected.txt out.txt
