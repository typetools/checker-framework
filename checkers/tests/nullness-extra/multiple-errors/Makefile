.PHONY: all

all:
	-rm Out.txt
	-$(JAVAC) -processor checkers.nullness.NullnessChecker *.java > Out.txt 2>&1
	diff -u Out.txt Expected.txt
