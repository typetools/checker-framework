.PHONY: all

all:
	mkdir -p bin
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker *.java -d bin/
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Delta.java -cp bin/ -d bin/
	rm bin/*.class
	rmdir bin
