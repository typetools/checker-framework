.PHONY: all test clean

all:
	-${JAVAC} -processor nullness OverrideMessage.java 2>&1 | grep -v '^Picked up JAVA_TOOL_OPTIONS: ' > OverrideMessage.actual 2>&1
	diff -u OverrideMessage.goal OverrideMessage.actual

test: all

clean:
	rm -f OverrideMessage.actual
