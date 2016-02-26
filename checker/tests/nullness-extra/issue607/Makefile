.PHONY: all

all: clean
	$(JAVAC) Issue607SuperClass.java Issue607Interface.java
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker -source 8 -target 8 Issue607.java

clean:
	-rm *.class