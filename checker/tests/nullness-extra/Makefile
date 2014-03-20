.PHONY: all Bug109 multiple-errors package-anno issue265 compat

all: Bug109 multiple-errors package-anno issue265 compat

Bug109:
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Bug109_A.java Bug109_B.java
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Bug109_B.java Bug109_A.java


multiple-errors:
	make -C multiple-errors

package-anno:
	make -C package-anno

issue265:
	make -C issue265

compat:
	make -C compat
