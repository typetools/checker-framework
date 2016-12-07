.PHONY: all

all: clean
	$(JAVAC) Issue607SuperClass.java Issue607Interface.java
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Issue607.java

clean:
	rm -f *.class
