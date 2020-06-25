JAVAVERSION := $(shell java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
CLASSPATH := ${CHECKERFRAMEWORK}/checker/dist/checker.jar:.
XBOOTCLASSPATH := ${CHECKERFRAMEWORK}/checker/dist/javac.jar
.PHONY: all

all:
	rm -f Out.txt Test.class LiveVariable.class
	javac -cp ${CHECKERFRAMEWORK}/checker/dist/checker.jar LiveVariable.java
    ifeq ($(JAVAVERSION), 8)
		java -Xbootclasspath/p:${XBOOTCLASSPATH} -cp ${CLASSPATH} LiveVariable > Out.txt 2>&1 || true
    else
		java -cp ${CLASSPATH} LiveVariable > Out.txt 2>&1 || true
    endif
	diff -u Expected.txt Out.txt
