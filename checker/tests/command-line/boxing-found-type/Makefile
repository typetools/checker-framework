.PHONY: all test clean

all:
	-${JAVAC} -processor org.checkerframework.checker.interning.InterningChecker BoxingFoundType.java 2>&1 | grep -v '^Picked up JAVA_TOOL_OPTIONS: ' > BoxingFoundType.actual 2>&1
	diff -u BoxingFoundType.goal BoxingFoundType.actual

test: all

clean:
	rm -f BoxingFoundType.actual
