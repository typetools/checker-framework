.PHONY: all

all:
	-rm Out.txt
	-$(JAVAC) -processor checkers.nullness.NullnessChecker -AprintErrorStack test/*.java > Out.txt 2>&1
	diff Out.txt Expected.txt
