.PHONY: all

all:
	rm -f Out.txt
	mkdir -p bin
	$(JAVAC) lib/*.java -d bin/
	rm -f bin/lib/Anno.class
	jar cvf lib.jar -C bin/ lib/
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Issue348.java -d bin/ -cp lib.jar > Out.txt 2>&1
	diff -u Expected.txt Out.txt
	rm Out.txt lib.jar bin/*.class bin/lib/*.class
	rmdir bin/lib bin/
