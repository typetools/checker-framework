.PHONY: all

all: clean
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Issue594.java > Out.txt 2>&1 || true
	diff -u Expected.txt Out.txt -I "illegal-access is deprecated"

clean:
	rm -f Issue594.class Out.txt
