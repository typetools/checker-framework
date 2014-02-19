.PHONY: all

all:
	-mkdir bin
	$(JAVAC) -processor checkers.nullness.NullnessChecker *.java -d bin/
	$(JAVAC) -processor checkers.nullness.NullnessChecker Delta.java -cp bin/ -d bin/
	rm bin/*.class
	rmdir bin
