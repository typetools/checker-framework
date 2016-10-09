# All tests: passing and failing
.PHONY: all skipped Bug109 multiple-errors package-anno issue265 issue309 compat shorthand issue502 issue559 issue594 issue607

# Tests that are currently passing
all: Bug109 compat issue265 issue309 issue502 multiple-errors package-anno shorthand issue607 issue594

# Tests that are currently not passing
skipped: issue559


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
