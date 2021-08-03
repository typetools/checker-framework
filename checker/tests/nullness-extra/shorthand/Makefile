.PHONY: all  nullnessOnly nullnessRegex nullnessBad nonsense

all: nullnessOnly nullnessRegex nullnessBad nonsense

nullnessOnly:
	rm -f Out.txt
	$(JAVAC) -processor NullnessChecker -XDrawDiagnostics -Anomsgtext -Awarns NullnessRegexWithErrors.java > Out.txt 2>&1 || true
	diff -u NullnessOnlyExpected.txt Out.txt

nullnessRegex:
	rm -f Out.txt
	$(JAVAC) -processor NullnessChecker,RegexChecker -XDrawDiagnostics -Anomsgtext -Awarns NullnessRegexWithErrors.java > Out.txt 2>&1 || true
	diff -u NullnessRegexExpected.txt Out.txt

nullnessBad:
	rm -f Out.txt
	$(JAVAC) -processor nullness.NullnessChecker -XDrawDiagnostics -Anomsgtext -Awarns NullnessRegexWithErrors.java > Out.txt 2>&1 || true
	diff -u NullnessBadExpected.txt Out.txt

nonsense:
	rm -f Out.txt
	$(JAVAC) -processor NonsenseChecker -XDrawDiagnostics -Anomsgtext -Awarns NullnessRegexWithErrors.java > Out.txt 2>&1 || true
	diff -u NonsenseExpected.txt Out.txt
