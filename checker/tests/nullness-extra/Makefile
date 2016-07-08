# Tests that are currently passing
PASSING_TESTS = Bug109 multiple-errors package-anno issue265 issue309 compat shorthand issue502 issue559 issue594 issue607
# Tests that are currently not passing
FAILING_TESTS = issue559

# Tests that are currently passing
all: ${PASSING_TESTS}

# Tests that are currently not passing
skipped: ${FAILING_TESTS}


Bug109:
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Bug109_A.java Bug109_B.java
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Bug109_B.java Bug109_A.java


compat:
	make -C compat

issue265:
	make -C issue265

issue309:
	make -C issue309

issue502:
	make -C issue502

issue559:
	make -C issue559

issue594:
	make -C issue594

multiple-errors:
	make -C multiple-errors

package-anno:
	make -C package-anno

shorthand:
	make -C shorthand

issue607:
	make -C issue607

# All tests: passing and failing
.PHONY: all skipped ${PASSING_TESTS}
