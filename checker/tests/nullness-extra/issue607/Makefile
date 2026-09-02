.PHONY: all test clean

all: clean
	$(JAVAC) Issue607SuperClass.java Issue607Interface.java
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Issue607.java

test: all

clean:
	@rm -f *.class
