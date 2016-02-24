.PHONY: all

all: clean
	$(JAVAC) -source 8 -target 8 Issue607SuperClass.java Issue607Interface.java
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker -source 8 -target 8 Issue607.java > Out.txt 2>&1
	diff -u Out.txt Expected.txt

clean:
	-rm *.class Out.txt
