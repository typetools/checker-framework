# Tests that are currently passing
PASSING_TESTS = Bug109 compat issue265 issue309 issue502 issue594 issue607 multiple-errors package-anno shorthand
ifneq (,$(findstring 1.7,$(shell javac -version)))
  PASSING_TESTS_JDK8 = issue559
else
  PASSING_TESTS_JDK8 =
endif

# Tests that are currently not passing
FAILING_TESTS =

# Tests that are currently passing
all: ${PASSING_TESTS} ${PASSING_TESTS_JDK8}

# Tests that are currently not passing
skipped: ${FAILING_TESTS}


Bug109:
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Bug109_A.java Bug109_B.java
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Bug109_B.java Bug109_A.java


compat:
	$(MAKE) -C compat

issue265:
	$(MAKE) -C issue265

issue309:
	$(MAKE) -C issue309

issue502:
	$(MAKE) -C issue502

issue559:
	$(MAKE) -C issue559

issue594:
	$(MAKE) -C issue594

multiple-errors:
	$(MAKE) -C multiple-errors

package-anno:
	$(MAKE) -C package-anno

shorthand:
	$(MAKE) -C shorthand

issue607:
	$(MAKE) -C issue607

# All tests: passing and failing
.PHONY: all skipped ${PASSING_TESTS}
