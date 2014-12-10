.PHONY: all Bug109 multiple-errors package-anno issue265 issue309 compat shorthand

all: Bug109 multiple-errors package-anno issue265 issue309 compat shorthand

Bug109:
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Bug109_A.java Bug109_B.java
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker Bug109_B.java Bug109_A.java


multiple-errors:
	make -C multiple-errors

package-anno:
	make -C package-anno

issue265:
	make -C issue265

issue309:
	make -C issue309

compat:
	make -C compat

shorthand:
	make -C shorthand