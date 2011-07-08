JAVAC?=../../checkers/binary/javac

FILES=Frequency.java \
  FrequencyRelations.java \
  Hz.java

all: compile check

compile: $(FILES)
	$(JAVAC) $(FILES)

check: compile $(FILES)
	$(JAVAC) -processor checkers.units.UnitsChecker -Aunits=Hz,Frequency Demo.java