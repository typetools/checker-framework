PHONY: all

all:
	$(JAVAC) -AjsonOutput=actual.json -processor org.checkerframework.checker.nullness.NullnessChecker JsonOutputTestCase.java > Out.txt 2>&1 || true
	sed -i.orig -E 's="file:///.*/checker/tests/="file:/checker/tests/=' actual.json
	diff expected.json actual.json
