.PHONY: all test clean

all:
	rm -f Out.txt
	-$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker *.java > Out.txt 2>&1
	diff -u Expected.txt Out.txt

test: all

clean:
	rm -f Out.txt
