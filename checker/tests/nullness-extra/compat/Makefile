.PHONY: all

all:
	rm -f Out.txt CompatTest.class lib/Lib.class javax/annotation/Nullable.class
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker lib/Lib.java javax/annotation/Nullable.java CompatTest.java > Out.txt 2>&1 || true
	diff --ignore-matching-lines="illegal-access is deprecated" -u Expected.txt Out.txt
