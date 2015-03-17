.PHONY: all  nullnessOnly nullnessRegex nullnessBad nonsense

all: nullnessOnly nullnessRegex nullnessBad nonsense

nullnessOnly:
	-rm Out.txt
	-$(JAVAC) -processor NullnessChecker -XDrawDiagnostics -Anomsgtext -Awarns -AprintErrorStack NullnessRegexWithErrors.java > Out.txt 2>&1
	diff -u Out.txt NullnessOnlyExpected.txt

nullnessRegex:
	-rm Out.txt
	-$(JAVAC) -processor NullnessChecker,RegexChecker -XDrawDiagnostics -Anomsgtext -Awarns -AprintErrorStack NullnessRegexWithErrors.java > Out.txt 2>&1
	diff -u Out.txt NullnessRegexExpected.txt

nullnessBad:
	-rm Out.txt
	-$(JAVAC) -processor nullness.NullnessChecker -XDrawDiagnostics -Anomsgtext -Awarns -AprintErrorStack NullnessRegexWithErrors.java > Out.txt 2>&1
	diff -u Out.txt NullnessBadExpected.txt

nonsense:
	-rm Out.txt
	-$(JAVAC) -processor NonsenseChecker -XDrawDiagnostics -Anomsgtext -Awarns -AprintErrorStack NullnessRegexWithErrors.java > Out.txt 2>&1
	diff -u Out.txt NonsenseExpected.txt

