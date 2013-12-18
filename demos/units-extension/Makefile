JAVAC?=../../checkers/binary/javac

FILES=Frequency.java \
  FrequencyRelations.java \
  Hz.java

all: compile check

compile: $(FILES)
	$(JAVAC) $(FILES)

check: compile $(FILES)
	@echo "***** This command is expected to produce errors on lines 8 and 29:"
	$(JAVAC) -Xbootclasspath/p:. -AprintErrorStack -processor checkers.units.UnitsChecker -Aunits=Hz,Frequency Demo.java
