.PHONY: all

all:
	rm -f Out.txt
	-$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker test/*.java > Out.txt 2>&1
	diff -u Expected.txt Out.txt
