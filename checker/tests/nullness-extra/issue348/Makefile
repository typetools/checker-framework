.PHONY: all

all:
	-rm Out.txt
	-mkdir bin
	$(JAVAC) lib/*.java -d bin/
	-rm bin/lib/Anno.class
	jar cvf lib.jar -C bin/ lib/
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Test.java -d bin/ -cp lib.jar > Out.txt 2>&1
	diff -u Out.txt Expected.txt
	rm Out.txt lib.jar bin/*.class bin/lib/*.class
	rmdir bin/lib bin/
