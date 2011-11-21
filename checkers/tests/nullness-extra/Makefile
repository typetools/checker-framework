.PHONY: all Bug109 multiple-errors

all: Bug109 multiple-errors

Bug109:
	$(JAVAC) -processor checkers.nullness.NullnessChecker Bug109_A.java Bug109_B.java
	$(JAVAC) -processor checkers.nullness.NullnessChecker Bug109_B.java Bug109_A.java


multiple-errors:
	make -C multiple-errors
