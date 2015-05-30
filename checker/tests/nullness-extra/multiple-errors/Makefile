.PHONY: all

all:
	-rm Out.txt
	-$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker *.java > Out.txt 2>&1
	diff -u Out.txt Expected.txt
