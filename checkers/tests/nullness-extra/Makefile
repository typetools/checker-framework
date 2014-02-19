.PHONY: all Bug109 multiple-errors package-anno issue265

all: Bug109 multiple-errors package-anno issue265

Bug109:
	$(JAVAC) -processor checkers.nullness.NullnessChecker Bug109_A.java Bug109_B.java
	$(JAVAC) -processor checkers.nullness.NullnessChecker Bug109_B.java Bug109_A.java


multiple-errors:
	make -C multiple-errors

package-anno:
	make -C package-anno

issue265:
	make -C issue265
