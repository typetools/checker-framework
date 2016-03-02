.PHONY: all

all: clean
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=myjdk.astub -AstubWarnIfOverwritesBytecode Issue559.java > Out.txt 2>&1
	diff -u Out.txt Expected.txt

clean:
	-rm Issue559.class Out.txt
