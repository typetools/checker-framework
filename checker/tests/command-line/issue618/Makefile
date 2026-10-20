.PHONY: all test clean

all:
	-${JAVAC} -processor regex,org.checkerframework.checker.tainting.TaintingChecker TwoCheckers.java 2>&1 | grep -v '^Picked up JAVA_TOOL_OPTIONS: ' > out.txt 2>&1
	diff -u expected.txt out.txt

test: all

clean:
	rm -f out.txt
