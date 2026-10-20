.PHONY: all test clean

all: clean
	-$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Issue594.java > Out.txt 2>&1
	diff -u Expected.txt Out.txt

test: all

clean:
	@rm -f Issue594.class Out.txt
