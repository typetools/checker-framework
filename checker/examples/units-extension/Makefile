FILES=Frequency.java \
  FrequencyRelations.java \
  Hz.java \
  kHz.java

JAVAOPTS=-source 7 -target 7

JSR308JAVAC?=../../bin/javac

all: framework-test clean

demo:
	$(JSR308JAVAC) $(JAVAOPTS) $(FILES)
	@echo "***** This command is expected to produce errors on line 10 & 61:"
	$(JSR308JAVAC) -Xbootclasspath/p:. -AprintErrorStack -processor org.checkerframework.checker.units.UnitsChecker -Aunits=Hz,kHz,Frequency Demo.java

framework-test:
	$(JSR308JAVAC) $(JAVAOPTS) $(FILES)
	-$(JSR308JAVAC) -Xbootclasspath/p:. -AprintErrorStack -processor org.checkerframework.checker.units.UnitsChecker -Aunits=Hz,kHz,Frequency Demo.java > Out.txt 2>&1
	diff -u Out.txt Expected.txt

clean:
	-rm *.class
	-rm Out.txt
