JAVA_FILES = org/jibble/pircbot/*.java

JAVAC ?= javac
JAVAC_COMMAND = ${JAVAC}

JAVAC_NULLNESS_ARGS = -processor checkers.nullness.NullnessChecker

ifdef JSR308
  CHECKERS ?= ${JSR308}/checker-framework/checkers
endif
CHECKERS_BOOTCLASSPATH = -Xbootclasspath/p:${CHECKERS}/jdk/jdk.jar

check-nullness:
	${JAVAC_COMMAND} ${JAVAC_NULLNESS_ARGS} ${CHECKERS_BOOTCLASSPATH} ${JAVA_FILES}

clean:
	rm -f org/jibble/pircbot/*.class
